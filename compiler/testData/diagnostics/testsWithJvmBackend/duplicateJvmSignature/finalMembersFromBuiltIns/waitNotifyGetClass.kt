// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

// KT-7174 Report error on members with the same signature as non-overridable methods from mapped Java types (like Object.wait/notify)

class A {
    <!ACCIDENTAL_OVERRIDE!>fun notify() {}<!>
    <!ACCIDENTAL_OVERRIDE!>fun notifyAll() {}<!>
    <!ACCIDENTAL_OVERRIDE!>fun wait() {}<!>
    <!ACCIDENTAL_OVERRIDE!>fun wait(l: Long) {}<!>
    <!ACCIDENTAL_OVERRIDE!>fun wait(l: Long, i: Int) {}<!>
    <!ACCIDENTAL_OVERRIDE!>fun getClass(): Class<Any> = null!!<!>
}

<!ACCIDENTAL_OVERRIDE!>fun notify() {}<!>
<!ACCIDENTAL_OVERRIDE!>fun notifyAll() {}<!>
<!ACCIDENTAL_OVERRIDE!>fun wait() {}<!>
<!ACCIDENTAL_OVERRIDE!>fun wait(l: Long) {}<!>
<!ACCIDENTAL_OVERRIDE!>fun wait(l: Long, i: Int) {}<!>
<!ACCIDENTAL_OVERRIDE!>fun getClass(): Class<Any> = null!!<!>
