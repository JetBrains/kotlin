annotation class A1
annotation class A2(val some: Int = 12)

class TopLevelClass<<!UNSUPPORTED!>@A1<!> <!UNSUPPORTED!>@A2(3)<!> <!UNSUPPORTED!>@A2<!> <!UNSUPPORTED!>@A1(12)<!> <!UNSUPPORTED!>@A2("Test")<!> T> {
    class InnerClass<<!UNSUPPORTED!>@A1<!> <!UNSUPPORTED!>@A2(3)<!> <!UNSUPPORTED!>@A2<!> <!UNSUPPORTED!>@A1(12)<!> <!UNSUPPORTED!>@A2("Test")<!> T> {
        fun test() {
            class InFun<<!UNSUPPORTED!>@A1<!> <!UNSUPPORTED!>@A2(3)<!> <!UNSUPPORTED!>@A2<!> <!UNSUPPORTED!>@A1(12)<!> <!UNSUPPORTED!>@A2("Test")<!> T>
        }
    }
}