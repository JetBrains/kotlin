package org.jetbrains.java.decompiler.util;

import java.util.HashMap;
import java.util.Map;

/**
 * A couple of helper methods with integers.
 *
 * @author SuperCoder79
 */
public final class IntHelper {
  // Special values for adjusting the string representation of ints.
  private static final Map<Integer, String> SPECIAL_VALUES = new HashMap<>();
  static {
    // Add color masking constants
    SPECIAL_VALUES.put(0xFF, "0xFF");
    SPECIAL_VALUES.put(0xFF00, "0xFF00");
    SPECIAL_VALUES.put(0xFF0000, "0xFF0000");
    SPECIAL_VALUES.put(0xFF000000, "0xFF000000");
  }

  private IntHelper() {}

  /**
   * Adjusts the string representation of an int to make it easier to read in certain cases, such as values used
   * to mask components of an RGB color.
   * @param value the input number
   * @return The adjusted string
   */
  public static String adjustedIntRepresentation(int value) {
    // Check against the special int values to see if we're one of those, and return
    String specialValue = SPECIAL_VALUES.get(value);

    if (specialValue != null) {
      return specialValue;
    }

    // Return the standard representation if we're not one of those
    return String.valueOf(value);
  }
}
