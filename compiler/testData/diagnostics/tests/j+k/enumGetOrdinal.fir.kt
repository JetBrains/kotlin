// FILE: MyEnum.java

public enum MyEnum {
    A;
    public int getOrdinal() {return "";}
}

// FILE: main.kt

fun foo() {
    MyEnum.A.getOrdinal()
}
