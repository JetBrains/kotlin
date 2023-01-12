public final class Container /* p1.Container*/ {
  public  Container();//  .ctor()

  public static final class MyNumber /* p1.Container.MyNumber*/ extends java.lang.Number {
    @java.lang.Override()
    public byte toByte();//  toByte()

    @java.lang.Override()
    public char toChar();//  toChar()

    @java.lang.Override()
    public double toDouble();//  toDouble()

    @java.lang.Override()
    public float toFloat();//  toFloat()

    @java.lang.Override()
    public int toInt();//  toInt()

    @java.lang.Override()
    public long toLong();//  toLong()

    @java.lang.Override()
    public short toShort();//  toShort()

    public  MyNumber();//  .ctor()
  }

  public static final class MyString /* p1.Container.MyString*/ implements java.lang.CharSequence {
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.CharSequence subSequence(int, int);//  subSequence(int, int)

    @java.lang.Override()
    public IntStream chars();//  chars()

    @java.lang.Override()
    public IntStream codePoints();//  codePoints()

    @java.lang.Override()
    public char get(int);//  get(int)

    @java.lang.Override()
    public int getLength();//  getLength()

    public  MyString();//  .ctor()
  }
}
