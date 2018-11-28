/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.jvm.deserialization;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.metadata.jvm.deserialization.UtfEncodingKt.MAX_UTF8_INFO_LENGTH;

public class BitEncoding {
    private static final boolean FORCE_8TO7_ENCODING;

    static {
        String use8to7;
        try {
            use8to7 = System.getProperty("kotlin.jvm.serialization.use8to7");
        }
        catch (SecurityException e) {
            use8to7 = null;
        }

        FORCE_8TO7_ENCODING = "true".equals(use8to7);
    }

    private static final char _8TO7_MODE_MARKER = (char) -1;

    private BitEncoding() {
    }

    /**
     * Converts a byte array of serialized data to an array of {@code String} satisfying JVM annotation value argument restrictions:
     * <ol>
     *     <li>Each string's length should be no more than 65535</li>
     *     <li>UTF-8 representation of each string cannot contain bytes in the range 0xf0..0xff</li>
     * </ol>
     */
    @NotNull
    public static String[] encodeBytes(@NotNull byte[] data) {
        // TODO: try both encodings here and choose the best one (with the smallest size)
        if (!FORCE_8TO7_ENCODING) {
            return UtfEncodingKt.bytesToStrings(data);
        }
        byte[] bytes = encode8to7(data);
        // Since 0x0 byte is encoded as two bytes in the Modified UTF-8 (0xc0 0x80) and zero is rather common to byte arrays, we increment
        // every byte by one modulo max byte value, so that the less common value 0x7f will be represented as two bytes instead.
        addModuloByte(bytes, 1);
        return splitBytesToStringArray(bytes);
    }

    /**
     * Converts a byte array to another byte array, every element of which is in the range 0x0..0x7f.
     *
     * The conversion is equivalent to the following: input bytes are combined into one long bit string. This big string is then split into
     * groups of 7 bits. Each resulting 7-bit chunk is then converted to a byte (with a leading bit = 0). The last chunk may have less than
     * 7 bits, it's prepended with zeros to form a byte. The result is then the array of these bytes, each of which is obviously in the
     * range 0x0..0x7f.
     *
     * Suppose the input of 4 bytes is given (bytes are listed from the beginning to the end, each byte from the least significant bit to
     * the most significant bit, bits within each byte are numbered):
     *
     *     01234567 01234567 01234567 01234567
     *
     * The output for this kind of input will be of the following form ('#' represents a zero bit):
     *
     *     0123456# 7012345# 6701234# 5670123# 4567####
     */
    @NotNull
    private static byte[] encode8to7(@NotNull byte[] data) {
        // ceil(data.length * 8 / 7)
        int resultLength = (data.length * 8 + 6) / 7;
        byte[] result = new byte[resultLength];

        // We maintain a pointer to the bit in the input, which is represented by two numbers: index of the current byte in the input and
        // the index of a bit inside this byte (0 is least significant, 7 is most significant)
        int byteIndex = 0;
        int bit = 0;

        // Write all resulting bytes except the last one. To do this we need to collect exactly 7 bits, starting from the current, into a
        // byte. In almost all cases these 7 bits can be collected from two parts: the first is several (at least one) most significant bits
        // from the current byte, the second is several (maybe zero) least significant bits from the next byte. The special case is when the
        // current bit is the first (least significant) bit in its byte (bit == 0): then the 7 needed bits are just the 7 least significant
        // of the current byte.
        for (int i = 0; i < resultLength - 1; i++) {
            if (bit == 0) {
                result[i] = (byte) (data[byteIndex] & 0x7f);
                bit = 7;
                continue;
            }

            int firstPart = (data[byteIndex] & 0xff) >>> bit;
            int newBit = (bit + 7) & 7;
            int secondPart = (data[++byteIndex] & ((1 << newBit) - 1)) << 8 - bit;
            result[i] = (byte) (firstPart + secondPart);
            bit = newBit;
        }

        // Write the last byte, which is just several most significant bits of the last byte in the input, padded with zeros
        if (resultLength > 0) {
            assert bit != 0 : "The last chunk cannot start from the input byte since otherwise at least one bit will remain unprocessed";
            assert byteIndex == data.length - 1 : "The last 7-bit chunk should be encoded from the last input byte: " +
                                                  byteIndex + " != " + (data.length - 1);
            result[resultLength - 1] = (byte) ((data[byteIndex] & 0xff) >>> bit);
        }

        return result;
    }

    private static void addModuloByte(@NotNull byte[] data, int increment) {
        for (int i = 0, n = data.length; i < n; i++) {
            data[i] = (byte) ((data[i] + increment) & 0x7f);
        }
    }

    /**
     * Converts a big byte array into the array of strings, where each string, when written to the constant pool table in bytecode, produces
     * a byte array of not more than MAX_UTF8_INFO_LENGTH. Each byte, except those which are 0x0, occupies exactly one byte in the constant
     * pool table. Zero bytes occupy two bytes in the table each.
     *
     * When strings are constructed from the array of bytes here, they are encoded in the platform's default encoding. This is fine: the
     * conversion to the Modified UTF-8 (which here would be equivalent to replacing each 0x0 with 0xc0 0x80) will happen later by ASM, when
     * it writes these strings to the bytecode
     */
    @NotNull
    private static String[] splitBytesToStringArray(@NotNull byte[] data) {
        List<String> result = new ArrayList<String>();

        // The offset where the currently processed string starts
        int off = 0;

        // The effective length the bytes of the current string would occupy in the constant pool table.
        // 2 because the first char is -1 which denotes the encoding mode and occupies two bytes in Modified UTF-8
        int len = 2;

        boolean encodingModeAdded = false;

        for (int i = 0, n = data.length; i < n; i++) {
            // When the effective length reaches at least MAX - 1, we add the current string to the result. Note that the effective length
            // is at most MAX here: non-zero bytes occupy 1 byte and zero bytes occupy 2 bytes, so we couldn't jump over more than one byte
            if (len >= MAX_UTF8_INFO_LENGTH - 1) {
                assert len <= MAX_UTF8_INFO_LENGTH : "Produced strings cannot contain more than " + MAX_UTF8_INFO_LENGTH + " bytes: " + len;
                String string = new String(data, off, i - off);
                if (!encodingModeAdded) {
                    encodingModeAdded = true;
                    result.add(_8TO7_MODE_MARKER + string);
                }
                else {
                    result.add(string);
                }
                off = i;
                len = 0;
            }

            if (data[i] == 0) {
                len += 2;
            }
            else {
                len++;
            }
        }

        if (len >= 0) {
            result.add(new String(data, off, data.length - off));
        }

        //noinspection SSBasedInspection
        return result.toArray(new String[result.size()]);
    }

    /**
     * Converts encoded array of {@code String} obtained by {@link BitEncoding#encodeBytes(byte[])} back to a byte array.
     */
    @NotNull
    public static byte[] decodeBytes(@NotNull String[] data) {
        if (data.length > 0 && !data[0].isEmpty()) {
            char possibleMarker = data[0].charAt(0);
            if (possibleMarker == UtfEncodingKt.UTF8_MODE_MARKER) {
                return UtfEncodingKt.stringsToBytes(dropMarker(data));
            }
            if (possibleMarker == _8TO7_MODE_MARKER) {
                data = dropMarker(data);
            }
        }

        byte[] bytes = combineStringArrayIntoBytes(data);
        // Adding 0x7f modulo max byte value is equivalent to subtracting 1 the same modulo, which is inverse to what happens in encodeBytes
        addModuloByte(bytes, 0x7f);
        return decode7to8(bytes);
    }

    @NotNull
    private static String[] dropMarker(@NotNull String[] data) {
        // Clone because the clients should be able to use the passed array for their own purposes.
        // This is cheap because the size of the array is 1 or 2 almost always.
        String[] result = data.clone();
        result[0] = result[0].substring(1);
        return result;
    }

    /**
     * Combines the array of strings resulted from encodeBytes() into one long byte array
     */
    @NotNull
    private static byte[] combineStringArrayIntoBytes(@NotNull String[] data) {
        int resultLength = 0;
        for (String s : data) {
            assert s.length() <= MAX_UTF8_INFO_LENGTH : "String is too long: " + s.length();
            resultLength += s.length();
        }

        byte[] result = new byte[resultLength];
        int p = 0;
        for (String s : data) {
            for (int i = 0, n = s.length(); i < n; i++) {
                result[p++] = (byte) s.charAt(i);
            }
        }

        return result;
    }

    /**
     * Decodes the byte array resulted from encode8to7().
     *
     * Each byte of the input array has at most 7 valuable bits of information. So the decoding is equivalent to the following: least
     * significant 7 bits of all input bytes are combined into one long bit string. This bit string is then split into groups of 8 bits,
     * each of which forms a byte in the output. If there are any leftovers, they are ignored, since they were added just as a padding and
     * do not comprise a full byte.
     *
     * Suppose the following encoded byte array is given (bits are numbered the same way as in encode8to7() doc):
     *
     *     01234567 01234567 01234567 01234567
     *
     * The output of the following form would be produced:
     *
     *     01234560 12345601 23456012
     *
     * Note how all most significant bits and leftovers are dropped, since they don't contain any useful information
     */
    @NotNull
    private static byte[] decode7to8(@NotNull byte[] data) {
        // floor(7 * data.length / 8)
        int resultLength = 7 * data.length / 8;

        byte[] result = new byte[resultLength];

        // We maintain a pointer to an input bit in the same fashion as in encode8to7(): it's represented as two numbers: index of the
        // current byte in the input and index of the bit in the byte
        int byteIndex = 0;
        int bit = 0;

        // A resulting byte is comprised of 8 bits, starting from the current bit. Since each input byte only "contains 7 bytes", a
        // resulting byte always consists of two parts: several most significant bits of the current byte and several least significant bits
        // of the next byte
        for (int i = 0; i < resultLength; i++) {
            int firstPart = (data[byteIndex] & 0xff) >>> bit;
            byteIndex++;
            int secondPart = (data[byteIndex] & ((1 << (bit + 1)) - 1)) << 7 - bit;
            result[i] = (byte) (firstPart + secondPart);

            if (bit == 6) {
                byteIndex++;
                bit = 0;
            }
            else {
                bit++;
            }
        }

        return result;
    }
}
