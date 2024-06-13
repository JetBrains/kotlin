// TARGET_BACKEND: JVM_IR

// MODULE: library

// FILE: EnumClassWithDefaultConstructor.java

public enum EnumClassWithDefaultConstructor {
    INSTANCE;
}

// FILE: EnumClassWithUserDefinedConstructor.java

public enum EnumClassWithUserDefinedConstructor {
    INSTANCE("");
    EnumClassWithUserDefinedConstructor(String value) {}
}

// MODULE: main(library)

// FILE: MainModuleJavaClass.java

import java.util.concurrent.TimeUnit;

public class MainModuleJavaClass {
    public void consumeEnumClassWithDefaultConstructor(EnumClassWithDefaultConstructor arg) {}
    public EnumClassWithDefaultConstructor getEnumClassWithDefaultConstructor() { return EnumClassWithDefaultConstructor.INSTANCE; }
    public void setEnumClassWithDefaultConstructor(EnumClassWithDefaultConstructor value) {}

    public void consumeEnumClassWithUserDefinedConstructor(EnumClassWithUserDefinedConstructor arg) {}
    public EnumClassWithUserDefinedConstructor getEnumClassWithUserDefinedConstructor() { return EnumClassWithUserDefinedConstructor.INSTANCE; }
    public void setEnumClassWithUserDefinedConstructor(EnumClassWithUserDefinedConstructor value) {}

    public void consumeTimeUnitA(java.util.concurrent.TimeUnit arg) {}
    public java.util.concurrent.TimeUnit getTimeUnitA() { return null; }
    public void setTimeUnitA(java.util.concurrent.TimeUnit value) {}

    public void consumeTimeUnitB(TimeUnit arg) {}
    public TimeUnit getTimeUnitB() { return null; }
    public void setTimeUnitB(TimeUnit value) {}
}

// FILE: kotlin.kt

import java.util.concurrent.TimeUnit

fun test(mainModuleJavaClass: MainModuleJavaClass) {
    mainModuleJavaClass.consumeEnumClassWithDefaultConstructor(EnumClassWithDefaultConstructor.INSTANCE)
    mainModuleJavaClass.enumClassWithDefaultConstructor = EnumClassWithDefaultConstructor.INSTANCE

    mainModuleJavaClass.consumeEnumClassWithUserDefinedConstructor(EnumClassWithUserDefinedConstructor.INSTANCE)
    mainModuleJavaClass.enumClassWithUserDefinedConstructor = EnumClassWithUserDefinedConstructor.INSTANCE

    mainModuleJavaClass.consumeTimeUnitA(java.util.concurrent.TimeUnit.DAYS)
    mainModuleJavaClass.consumeTimeUnitA(TimeUnit.DAYS)
    mainModuleJavaClass.timeUnitA = java.util.concurrent.TimeUnit.DAYS
    mainModuleJavaClass.timeUnitA = TimeUnit.DAYS

    mainModuleJavaClass.consumeTimeUnitB(java.util.concurrent.TimeUnit.DAYS)
    mainModuleJavaClass.consumeTimeUnitB(TimeUnit.DAYS)
    mainModuleJavaClass.timeUnitB = java.util.concurrent.TimeUnit.DAYS
    mainModuleJavaClass.timeUnitB = TimeUnit.DAYS
}

fun box(): String {
    test(MainModuleJavaClass())
    return "OK"
}
