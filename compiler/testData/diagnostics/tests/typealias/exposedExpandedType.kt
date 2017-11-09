// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

typealias L<T> = List<T>

class Outer {
    private class Private
    protected class Protected
    internal class Internal

    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestPrivate1<!> = Private
    protected typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestPrivate2<!> = Private
    internal typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestPrivate3<!> = Private
    private typealias TestPrivate4 = Private
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestPrivate5<!> = L<Private>
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestPrivate6<!> = L<TestPrivate1>

    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestProtected1<!> = Protected
    protected typealias TestProtected2 = Protected
    internal typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestProtected3<!> = Protected
    private typealias TestProtected4 = Protected
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestProtected5<!> = L<Protected>
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestProtected6<!> = L<TestProtected1>
    
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestInternal1<!> = Internal
    protected typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestInternal2<!> = Internal
    internal typealias TestInternal3 = Internal
    private typealias TestInternal4 = Internal
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestInternal5<!> = L<Internal>
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestInternal6<!> = L<TestInternal1>
}

private class Private
internal class Internal

typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestPrivate1<!> = Private
internal typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestPrivate2<!> = Private
private typealias TestPrivate3 = Private
typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestPrivate4<!> = L<Private>
typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestPrivate5<!> = L<TestPrivate1>

typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestInternal1<!> = Internal
internal typealias TestInternal2 = Internal
private typealias TestInternal3 = Internal
typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestInternal4<!> = L<Internal>
typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TestInternal5<!> = L<TestInternal1>
