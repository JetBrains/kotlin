// ISSUE: KT-29316
// ISSUE: KT-24284

class A
class B
fun main() {
    <!EQUALITY_NOT_APPLICABLE!>A() == B()<!>
    <!EQUALITY_NOT_APPLICABLE!>A() === B()<!>
}
