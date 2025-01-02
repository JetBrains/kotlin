// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.util.lang;

import com.intellij.ReviseWhenPortedToJDK;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>NB: copied from intellij to workaround KT-73967.</b>
 *
 * <p>Can be removed as soon as Kotlin depends on intellij of at least 243.23654.3.</p>
 *
 *
 *
 * <p>A class representing a version of some Java platform - e.g. the runtime the class is loaded into, or some installed JRE.</p>
 *
 * <p>Based on <a href="http://openjdk.org/jeps/322">JEP 322 "Time-Based Release Versioning"</a> (Java 10+), but also supports JEP 223
 * "New Version-String Scheme" (Java 9), as well as earlier version's formats.</p>
 *
 * <p>See {@link #parse(String)} for examples of supported version strings.</p>
 *
 * @implNote the class is used in bootstrap - please use only JDK API
 */
@SuppressWarnings("DuplicatedCode")
public final class JavaVersion implements Comparable<JavaVersion> {
  /**
   * The major version.
   * Corresponds to the first number of the 9+ format (<b>9</b>.0.1) / the second number of the 1.x format (1.<b>8</b>.0_60).
   */
  public final int feature;

  /**
   * The minor version.
   * Corresponds to the second number of the 9+ format (9.<b>0</b>.1) / the third number of 1.x the format (1.8.<b>0</b>_60).
   * Was used in version strings prior to 1.5, in newer strings is always {@code 0}.
   */
  public final int minor;

  /**
   * The patch version.
   * Corresponds to the third number of the 9+ format (9.0.<b>1</b>) / the number after an underscore of the 1.x format (1.8.0_<b>60</b>).
   */
  public final int update;

  /**
   * The build number.
   * Corresponds to a number prefixed by the "plus" sign in the 9+ format (9.0.1+<b>7</b>) /
   * by "-b" string in the 1.x format (1.8.0_60-b<b>12</b>).
   */
  public final int build;

  /**
   * {@code true} if the platform is an early access release, {@code false} otherwise (or when not known).
   */
  public final boolean ea;

  private JavaVersion(int feature, int minor, int update, int build, boolean ea) {
    this.feature = feature;
    this.minor = minor;
    this.update = update;
    this.build = build;
    this.ea = ea;
  }

  @Override
  public int compareTo(@NotNull JavaVersion o) {
    int diff = feature - o.feature;
    if (diff != 0) return diff;
    diff = minor - o.minor;
    if (diff != 0) return diff;
    diff = update - o.update;
    if (diff != 0) return diff;
    diff = build - o.build;
    if (diff != 0) return diff;
    return (ea ? 0 : 1) - (o.ea ? 0 : 1);
  }

  public boolean isAtLeast(int feature) {
    return this.feature >= feature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof JavaVersion)) return false;
    JavaVersion other = (JavaVersion)o;
    return feature == other.feature &&
           minor == other.minor &&
           update == other.update &&
           build == other.build &&
           ea == other.ea;
  }

  @Override
  public int hashCode() {
    int hash = feature;
    hash = 31 * hash + minor;
    hash = 31 * hash + update;
    hash = 31 * hash + build;
    hash = 31 * hash + (ea ? 1231 : 1237);
    return hash;
  }

  /**
   * @return feature version string, e.g. <b>1.8</b> or <b>11</b>
   */
  public @NotNull String toFeatureString() {
    return formatVersionTo(true, true);
  }

  /**
   * @return feature, minor and update components of the version string, e.g.
   * <b>1.8.0_242</b> or <b>11.0.5</b>
   */
  public @NotNull String toFeatureMinorUpdateString() {
    return formatVersionTo(false, true);
  }

  @Override
  public String toString() {
    return formatVersionTo(false, false);
  }

  private String formatVersionTo(boolean upToFeature, boolean upToUpdate) {
    StringBuilder sb = new StringBuilder();
    if (feature > 8) {
      sb.append(feature);
      if (!upToFeature) {
        if (minor > 0 || update > 0) sb.append('.').append(minor);
        if (update > 0) sb.append('.').append(update);
        if (!upToUpdate) {
          if (ea) sb.append("-ea");
          if (build > 0) sb.append('+').append(build);
        }
      }
    }
    else {
      sb.append("1.").append(feature);
      if (!upToFeature) {
        if (minor > 0 || update > 0 || ea || build > 0) sb.append('.').append(minor);
        if (update > 0) sb.append('_').append(update);
        if (!upToUpdate) {
          if (ea) sb.append("-ea");
          if (build > 0) sb.append("-b").append(build);
        }
      }
    }
    return sb.toString();
  }

  /**
   * Composes a version object out of given parameters.
   *
   * @throws IllegalArgumentException when any of the numbers is negative
   */
  public static @NotNull JavaVersion compose(int feature, int minor, int update, int build, boolean ea) throws IllegalArgumentException {
    if (feature < 0) throw new IllegalArgumentException();
    if (minor < 0) throw new IllegalArgumentException();
    if (update < 0) throw new IllegalArgumentException();
    if (build < 0) throw new IllegalArgumentException();
    return new JavaVersion(feature, minor, update, build, ea);
  }

  public static @NotNull JavaVersion compose(int feature) {
    return compose(feature, 0, 0, 0, false);
  }

  private static JavaVersion current;

  /**
   * Returns the version of a Java runtime the class is loaded into.
   * The method attempts to parse {@code "java.runtime.version"} system property first (usually, it is more complete),
   * and falls back to {@code "java.version"} if the former is invalid or differs in {@link #feature} or {@link #minor} numbers.
   */
  public static @NotNull JavaVersion current() {
    if (current == null) {
      JavaVersion fallback = parse(System.getProperty("java.version"));
      JavaVersion rt = rtVersion();
      if (rt == null) {
        try { rt = parse(System.getProperty("java.runtime.version")); }
        catch (Throwable ignored) { }
      }
      current = rt != null && rt.feature == fallback.feature && rt.minor == fallback.minor ? rt : fallback;
    }
    return current;
  }

  /**
   * Attempts to use Runtime.version() method available since Java 9.
   */
  @ReviseWhenPortedToJDK("9")
  private static @Nullable JavaVersion rtVersion() {
    try {
      Object version = Runtime.class.getMethod("version").invoke(null);
      int major = (Integer)version.getClass().getMethod("major").invoke(version);
      int minor = (Integer)version.getClass().getMethod("minor").invoke(version);
      int security = (Integer)version.getClass().getMethod("security").invoke(version);
      Object buildOpt = version.getClass().getMethod("build").invoke(version);
      int build = (Integer)buildOpt.getClass().getMethod("orElse", Object.class).invoke(buildOpt, Integer.valueOf(0));
      Object preOpt = version.getClass().getMethod("pre").invoke(version);
      boolean ea = (Boolean)preOpt.getClass().getMethod("isPresent").invoke(preOpt);
      return new JavaVersion(major, minor, security, build, ea);
    }
    catch (Throwable ignored) {
      return null;
    }
  }

  private static final int MAX_ACCEPTED_VERSION = 25;  // sanity check

  /**
   * <p>Parses a Java version string.</p>
   *
   * <p>Supports various sources, including (but not limited to):<br>
   *   - {@code "java.*version"} system properties (a version number without any decoration)<br>
   *   - values of Java compiler -source/-target/--release options ("$MAJOR", "1.$MAJOR")<br>
   *   - output of "{@code java -version}" (usually "java version \"$VERSION\"")<br>
   *   - a second line of the above command (something like to "Java(TM) SE Runtime Environment (build $VERSION)")<br>
   *   - output of "{@code java --full-version}" ("java $VERSION")<br>
   *   - a line of "release" file ("JAVA_VERSION=\"$VERSION\"")</p>
   *
   * <p>See com.intellij.util.lang.JavaVersionTest for examples.</p>
   *
   * @throws IllegalArgumentException if failed to recognize the number.
   */
  public static @NotNull JavaVersion parse(@NotNull String versionString) throws IllegalArgumentException {
    // trimming
    String str = versionString.trim();
    Map<String, String> trimmingMap = new HashMap<>(); // "substring to detect" to "substring from which to trim"
    trimmingMap.put("Runtime Environment", "(build ");
    trimmingMap.put("OpenJ9", "version ");
    for (String keyToDetect : trimmingMap.keySet()) {
      if (str.contains(keyToDetect)) {
        int p = str.indexOf(trimmingMap.get(keyToDetect));
        if (p > 0) str = str.substring(p);
      }
    }

    // partitioning
    List<String> numbers = new ArrayList<>(), separators = new ArrayList<>();
    int length = str.length(), p = 0;
    boolean number = false;
    while (p < length) {
      int start = p;
      while (p < length && Character.isDigit(str.charAt(p)) == number) p++;
      String part = str.substring(start, p);
      (number ? numbers : separators).add(part);
      number = !number;
    }

    // parsing
    if (!numbers.isEmpty() && !separators.isEmpty()) {
      try {
        int feature = Integer.parseInt(numbers.get(0)), minor = 0, update = 0, build = 0;
        boolean ea = false;

        if (feature >= 5 && feature < MAX_ACCEPTED_VERSION) {
          // Java 9+; Java 5+ (short format)
          p = 1;
          while (p < separators.size() && ".".equals(separators.get(p))) p++;
          if (p > 1 && numbers.size() > 2) {
            minor = Integer.parseInt(numbers.get(1));
            update = Integer.parseInt(numbers.get(2));
          }
          if (p < separators.size()) {
            String s = separators.get(p);
            if (s != null && !s.isEmpty() && s.charAt(0) == '-') {
              ea = startsWithWord(s, "-ea") || startsWithWord(s, "-internal");
              if (p < numbers.size() && s.charAt(s.length() - 1) == '+') {
                build = Integer.parseInt(numbers.get(p));
              }
              p++;
            }
            if (build == 0 && p < separators.size() && p < numbers.size() && "+".equals(separators.get(p))) {
              build = Integer.parseInt(numbers.get(p));
            }
          }
          return new JavaVersion(feature, minor, update, build, ea);
        }
        else if (feature == 1 && numbers.size() > 1 && separators.size() > 1 && ".".equals(separators.get(1))) {
          // Java 1.0 .. 1.4; Java 5+ (prefixed format)
          feature = Integer.parseInt(numbers.get(1));
          if (feature <= MAX_ACCEPTED_VERSION) {
            if (numbers.size() > 2 && separators.size() > 2 && ".".equals(separators.get(2))) {
              minor = Integer.parseInt(numbers.get(2));
              if (numbers.size() > 3 && separators.size() > 3 && "_".equals(separators.get(3))) {
                update = Integer.parseInt(numbers.get(3));
                if (separators.size() > 4) {
                  String s = separators.get(4);
                  if (s != null && !s.isEmpty() && s.charAt(0) == '-') {
                    ea = startsWithWord(s, "-ea") || startsWithWord(s, "-internal");
                  }
                  p = 4;
                  while (p < separators.size() && !separators.get(p).endsWith("-b")) p++;
                  if (p < numbers.size()) {
                    build = Integer.parseInt(numbers.get(p));
                  }
                }
              }
            }
            return new JavaVersion(feature, minor, update, build, ea);
          }
        }
      }
      catch (NumberFormatException ignored) { }
    }

    throw new IllegalArgumentException(versionString);
  }

  private static boolean startsWithWord(String s, String word) {
    return s.startsWith(word) && (s.length() == word.length() || !Character.isLetterOrDigit(s.charAt(word.length())));
  }

  /**
   * A safe version of {@link #parse(String)} - returns {@code null} when unable to parse a version string.
   */
  public static @Nullable JavaVersion tryParse(String versionString) {
    if (versionString != null) {
      try {
        return parse(versionString);
      }
      catch (IllegalArgumentException ignored) { }
    }

    return null;
  }
}
