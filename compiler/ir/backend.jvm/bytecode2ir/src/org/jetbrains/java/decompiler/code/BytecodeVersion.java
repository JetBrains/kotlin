package org.jetbrains.java.decompiler.code;

public final class BytecodeVersion implements Comparable<BytecodeVersion> {
  public final int major;
  public final int minor;

  public BytecodeVersion(int major, int minor) {
    this.major = major & 0xffff;
    this.minor = minor & 0xffff;
  }

  public boolean hasEnums() {
    return major >= MAJOR_5;
  }

  public boolean hasInvokeDynamic() {
    return major >= MAJOR_7;
  }

  public boolean hasLambdas() {
    return major >= MAJOR_8;
  }

  public boolean hasIndyStringConcat() {
    return major >= MAJOR_9;
  }

  public boolean hasOverride() {
    return major >= MAJOR_5;
  }

  public boolean hasJsr() {
    return major <= MAJOR_6;
  }

  public boolean hasIfPatternMatching() {
    return major >= MAJOR_16;
  }

  public boolean hasSwitchExpressions() {
    return major >= MAJOR_16;
  }

  public boolean hasSwitchPatternMatch() {
    return previewFrom(MAJOR_17);
  }

  public boolean hasSealedClasses() {
    return previewReleased(MAJOR_15, MAJOR_17);
  }

  public boolean hasLocalEnumsAndInterfaces() {
    return major >= MAJOR_16;
  }

  // Java 4 class references
  public boolean has14ClassReferences() {
    return major <= MAJOR_1_4;
  }

  public boolean hasNewTryWithResources() {
    return major >= MAJOR_11;
  }

  public boolean predatesJava() {
    return major <= MAJOR_1_0_2 && minor <= 2;
  }

  private boolean previewFrom(int previewStart) {
    return major >= previewStart && minor == PREVIEW;
  }

  private boolean previewReleased(int previewStart, int releaseMajor) {
    return major >= releaseMajor || previewFrom(previewStart);
  }

  @Override
  public int compareTo(BytecodeVersion o) {
    int cmp = Integer.compare(major, o.major);
    if (cmp != 0) return cmp;
    return Integer.compare(minor, o.minor);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BytecodeVersion that = (BytecodeVersion) o;
    return major == that.major && minor == that.minor;
  }

  @Override
  public int hashCode() {
    return major << 16 | minor;
  }

  public static final int PREVIEW = 65535;
  public static final int MAJOR_1_0_2 = 45;
  public static final int MAJOR_1_2 = 46;
  public static final int MAJOR_1_3 = 47;
  public static final int MAJOR_1_4 = 48;
  public static final int MAJOR_5 = 49;
  public static final int MAJOR_6 = 50;
  public static final int MAJOR_7 = 51;
  public static final int MAJOR_8 = 52;
  public static final int MAJOR_9 = 53;
  public static final int MAJOR_10 = 54;
  public static final int MAJOR_11 = 55;
  public static final int MAJOR_12 = 56;
  public static final int MAJOR_13 = 57;
  public static final int MAJOR_14 = 58;
  public static final int MAJOR_15 = 59;
  public static final int MAJOR_16 = 60;
  public static final int MAJOR_17 = 61;
}
