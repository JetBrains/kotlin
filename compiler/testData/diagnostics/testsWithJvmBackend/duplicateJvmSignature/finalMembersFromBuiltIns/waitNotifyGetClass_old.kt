// !DIAGNOSTICS: -UNUSED_PARAMETER
// TARGET_BACKEND: JVM_OLD

// KT-7174 Report error on members with the same signature as non-overridable methods from mapped Java types (like Object.wait/notify)

class A {
    <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>fun notify()<!> {}
    <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>fun notifyAll()<!> {}
    <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>fun wait()<!> {}
    <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>fun wait(l: Long)<!> {}
    <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>fun wait(l: Long, i: Int)<!> {}
    <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>fun getClass(): Class<Any><!> = null!!
}

<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>fun notify()<!> {}
<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>fun notifyAll()<!> {}
<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>fun wait()<!> {}
<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>fun wait(l: Long)<!> {}
<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>fun wait(l: Long, i: Int)<!> {}
<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>fun getClass(): Class<Any><!> = null!!
