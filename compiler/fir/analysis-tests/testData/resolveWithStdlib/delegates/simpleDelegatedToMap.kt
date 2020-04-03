class C(val map: MutableMap<String, Any>) {
    // NB: this does not work because of @LowPriorityInOverloadResolution not deserialized (KT-37228)
    var foo by <!AMBIGUITY!>map<!>
}

var bar by <!AMBIGUITY!>hashMapOf<String, Any>()<!>