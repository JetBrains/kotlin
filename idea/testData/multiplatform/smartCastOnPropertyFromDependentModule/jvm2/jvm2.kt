fun test(fromCommon: CommonDataClass, fromJvm: JvmDataClass) {
    if (fromCommon.property != null) {
        <!SMARTCAST_IMPOSSIBLE!>fromCommon.property<!>.doSomething()
    }

    if (fromJvm.property != null) {
        <!SMARTCAST_IMPOSSIBLE!>fromJvm.property<!>.doSomething()
    }
}