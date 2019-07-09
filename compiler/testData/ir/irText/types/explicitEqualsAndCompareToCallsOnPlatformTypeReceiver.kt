// FILE: explicitEqualsAndCompareToCallsOnPlatformTypeReceiver.kt

fun JavaClass.testPlatformEqualsPlatform() =
    null0().equals(null0())

fun JavaClass.testPlatformEqualsKotlin() =
    null0().equals(0.0)

fun JavaClass.testKotlinEqualsPlatform() =
    0.0.equals(null0())

fun JavaClass.testPlatformCompareToPlatform() =
    null0().compareTo(null0())

fun JavaClass.testPlatformCompareToKotlin() =
    null0().compareTo(0.0)

fun JavaClass.testKotlinCompareToPlatform() =
    0.0.compareTo(null0())

// FILE: JavaClass.java

public class JavaClass {
    public Double null0(){
        return null;
    }

}