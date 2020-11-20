annotation class A1
annotation class A2(val some: Int = 12)

class TopLevelClass<@A1 @A2(3) @A2 <!INAPPLICABLE_CANDIDATE!>@A1(12)<!> <!INAPPLICABLE_CANDIDATE!>@A2("Test")<!> T> {
    class InnerClass<@A1 @A2(3) @A2 <!INAPPLICABLE_CANDIDATE!>@A1(12)<!> <!INAPPLICABLE_CANDIDATE!>@A2("Test")<!> T> {
        fun test() {
            class InFun<@A1 @A2(3) @A2 <!INAPPLICABLE_CANDIDATE!>@A1(12)<!> <!INAPPLICABLE_CANDIDATE!>@A2("Test")<!> T>
        }
    }
}
