package test

fun main() {
    val e = Expect()

    e.platformFun()
    e.commonFun()

    topLevelPlatformFun()
    topLevelCommonFun()
}