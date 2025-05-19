// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-17460

interface A1 {
    fun foo()
}

interface A2 {
    suspend fun foo() {}
}

interface A3 {
    suspend fun foo()
}

class B1 : A1 {
    override suspend fun <!NON_SUSPEND_OVERRIDDEN_BY_SUSPEND!>foo<!>() {}
}

class B2 : A2 {
    override fun <!SUSPEND_OVERRIDDEN_BY_NON_SUSPEND!>foo<!>() {}
}

<!CONFLICTING_INHERITED_MEMBERS, MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class B3<!> : A1, A2

class B4 : A1, A2 {
    override fun <!CONFLICTING_INHERITED_MEMBERS!>foo<!>() {}
}

class B5 : A1, A2 {
    override suspend fun <!CONFLICTING_INHERITED_MEMBERS!>foo<!>() {}
}

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class B6<!> : A2, A3

<!CONFLICTING_INHERITED_MEMBERS, MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class B7<!> : A1, A2, A3

class B8 : A1, A2, A3 {
    override fun <!CONFLICTING_INHERITED_MEMBERS!>foo<!>() {}
}

class B9 : A1, A2, A3 {
    override suspend fun <!CONFLICTING_INHERITED_MEMBERS!>foo<!>() {}
}

interface I {
    suspend fun component1(): Int
}

data class DC(val <!SUSPEND_OVERRIDDEN_BY_NON_SUSPEND!>v<!>: Int) : I
