package rendererTest

private enum class TheEnum(val rgb: Int) {
    VAL1: TheEnum(0xFF0000)
}

//package rendererTest
//private final enum class TheEnum : kotlin.Enum<rendererTest.TheEnum> defined in rendererTest
//private constructor TheEnum(rgb: kotlin.Int) defined in rendererTest.TheEnum
//value-parameter val rgb: kotlin.Int defined in rendererTest.TheEnum.<init>
//public enum entry VAL1 : rendererTest.TheEnum defined in rendererTest.TheEnum
//private constructor VAL1() defined in rendererTest.TheEnum.VAL1
