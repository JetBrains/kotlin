package luhnybin

import java.util.*

fun main(args : Array<String>) {
  run()
  System.out?.println("Success")
}

// HELPER FUNCTIONS

val MASK : Char = 'X'

val MIN_LENGTH : Int = 14
val MAX_LENGTH : Int = 16

val random = Random(0xDEADBEEF)

class test(val testName : String) {
  var input : String = ""

  fun send(input : String) : test {
    this.input = input
    return this
  }

  fun expect(expected : String) {
    val actual = applyMask(input)

    assertEquals(
    actual, expected,
    "${testName} failed on \"${input}\"\n" +
    "applyMask(data) = \"${actual}\", but must be \"$expected\"")
  }

  fun sendAndExpect(input : String) = send(input).expect(input)

  fun assertEquals<T>(actual : T?, expected : T?, message : Any? = null) {
    if (actual != expected) {
      if (message == null)
        throw AssertionError()
      else
        throw AssertionError(message)
    }
  }
}

fun run() {
  test("line feed preservation").sendAndExpect("LF only ->\n<- LF only")

  for (i in MIN_LENGTH..MAX_LENGTH)
    test("valid " + i + "-digit #")
    .send(randomNumber(i))
    .expect(mask(i))

  for (i in MIN_LENGTH..MAX_LENGTH)
    test("non-matching " + i + "-digit #").sendAndExpect(nonMatchingSequence(i))

  test("not enough digits").sendAndExpect(nonMatchingSequence(MIN_LENGTH - 1))

  var tooMany = nonMatchingSequence(MAX_LENGTH)
  tooMany += computeLast(tooMany)
  test("too many digits").sendAndExpect(tooMany)

  test("14-digit # prefixed with 0s")
  .send("00" + randomNumber(14))
  .expect(mask(16))

  test("2 non-matching digits followed by a 14-digit #")
  .send("1256613959932537")
  .expect("12XXXXXXXXXXXXXX")

  test("14-digit # embedded in a 16-digit #")
  .send(nestedNumber())
  .expect(mask(16))

  test("16-digit # flanked by non-matching digits")
  .send("9875610591081018250321")
  .expect("987XXXXXXXXXXXXXXXX321")

  testFormatted(' ')
  testFormatted('-')

  test("exception message containing a card #")
  .send("java.lang.FakeException: " + formattedNumber(' ') + " is a card #.")
  .expect("java.lang.FakeException: " + formattedMask(' ') + " is a card #.")

  test("non-matching message").sendAndExpect("4111 1111 1111 111 doesn't have enough digits.")

  test("non-matching message").sendAndExpect("56613959932535089 has too many digits.")

  test("sequence of zeros")
  .send(repeatingSequence('0', 1000))
  .expect(mask(1000))

  test("long sequence of non-digits").sendAndExpect(nonDigits())

  testOverlappingMatches()

  test("long sequence of digits with no matches").sendAndExpect(nonMatchingSequence(1000))
}

fun formatNumber(number : String, delimiter : Char) : String {
  if (number.length != 16)
    throw IllegalArgumentException("Expected length of 16.")

  val formatted = StringBuilder()
  for (i in 0..3) {
    formatted.append(number.substring(i * 4, (i * 4) + 4))
    if (i < 3)
      formatted.append(delimiter)
  }

  return formatted.toString() ?: ""
}

fun formattedNumber(delimiter : Char) : String = formatNumber(randomNumber(16), delimiter)

fun checkDigit(c : Char) : Char {
  if (c < '0' || c > '9')
    throw IllegalArgumentException("Not a digit: " + c)

  return c
}

fun intValue(c : Char) : Int = checkDigit(c) - '0'

/** Computes the last digit necessary to pass the Luhn check. */
fun computeLast(allButLast : String) : Char {
  var sum = 0

  var i = allButLast.length - 1
  while (i >= 0) {
    val value = intValue(allButLast[i]) shl 1
    sum += if (value > 9) value - 9 else  value

    i -= 2
  }

  var j = allButLast.length - 2
  while (j >= 0) {
    sum += intValue(allButLast[j])
    j -= 2
  }

  var remainder = sum % 10
  return if (remainder == 0) '0' else ('0' + (10 - remainder)).chr
}

fun computeLast(allButLast : CharSequence) : Char = computeLast(allButLast.toString().sure())

fun setRandomDigits(builder : StringBuilder, start : Int, end : Int) {
  for (i in start..end-1)
    builder.setCharAt(i, randomDigit())
}

/** Generates a random digit. */
fun randomDigit() : Char = ('0' + random.nextInt(10)).chr

fun nonDigits() : String {
  val nonDigits = StringBuilder()
  for (i in 0..999)
    nonDigits.append((random.nextInt(68) + ':').chr)
  return nonDigits.toString().sure()
}

fun testFormatted(delimiter : Char) {
  test("16-digit # delimited with '" + delimiter + "'")
  .send(formattedNumber(delimiter))
  .expect(formattedMask(delimiter))
}

fun formattedMask(delimiter : Char) : String {
  val mask = StringBuilder()

  for (i in 0..3) {
    mask.append(repeatingSequence(MASK, 4))
    if (i < 3)
      mask.append(delimiter)
  }

  return mask.toString().sure()
}

/** Computes a random, valid card # with the specified number of digits. */
fun randomNumber(digits : Int) : String {
  val number = StringBuilder(digits)
  number.setLength(digits)
  setRandomDigits(number, 0, digits - 1)
  number.setCharAt(digits - 1, computeLast(number.subSequence(0, digits - 1).sure()))
  return number.toString() ?: ""
}

/** Creates a 16-digit card # with a 14-digit number embedded inside. */
fun nestedNumber() : String {
  val number = StringBuilder(16)
  number.setLength(16);
  setRandomDigits(number, 0, 14);
  number.setCharAt(14, computeLast(number.subSequence(1, 14).sure()))
  number.setCharAt(15, computeLast(number.subSequence(0, 15).sure()))
  return number.toString().sure()
}

/** Creates a sequence of mask characters with the given length. */
fun mask(length : Int) : String {
  return repeatingSequence(MASK, length);
}

/** Creates a sequence of c with the given length. */
fun repeatingSequence(c : Char, length : Int) : String {
  val sb = StringBuilder()
  for (i in 1..length)
    sb.append(c)
  return sb.toString().sure()
}

class DigitSet() {
  val bitSet = BitSet()

  fun add(digit : Char) = bitSet.set(intValue(digit))
  fun contains(digit : Char) : Boolean = bitSet.get(intValue(digit))
  fun clear() = bitSet.clear()
}

fun testOverlappingMatches() {
  val output = StringBuilder(randomNumber(MAX_LENGTH))
  for (i in 0..1000 - MAX_LENGTH - 1)
    output.append(computeLast(output.subSequence(i + 1, i + MAX_LENGTH).sure()))

  test("long sequence of overlapping, valid #s")
  .send(output.toString().sure())
  .expect(mask(output.length()));
}

/** Generates a sequence of digits with the specified length and no card #s. */
fun nonMatchingSequence(length : Int) : String {
  val builder = StringBuilder()

  for (lastIndex in 0..length-1) {
    val excluded = DigitSet()
    excluded.clear();

    // Compute digits that would result in valid card #s.
    for (subLength in MIN_LENGTH..MAX_LENGTH) {
      val start = lastIndex - (subLength - 1);
      if (start < 0)
        break;
      excluded.add(computeLast(builder.subSequence(start, lastIndex).sure()));
    }

    // Find a digit that doesn't result in a valid card #.
    var digit : Char = '0'
    do {
      digit = randomDigit()
    } while (excluded.contains(digit))
    builder.append(digit);
  }

  return builder.toString().sure()
}
