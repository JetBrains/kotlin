// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

typealias L<T> = List<T>

class Outer {
    private class Private
    protected class Protected
    internal class Internal

    <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>typealias TestPrivate1 = Private<!>
    <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>protected typealias TestPrivate2 = Private<!>
    <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>internal typealias TestPrivate3 = Private<!>
    private typealias TestPrivate4 = Private
    <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>typealias TestPrivate5 = L<Private><!>
    typealias TestPrivate6 = L<TestPrivate1>

    <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>typealias TestProtected1 = Protected<!>
    protected typealias TestProtected2 = Protected
    <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>internal typealias TestProtected3 = Protected<!>
    private typealias TestProtected4 = Protected
    <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>typealias TestProtected5 = L<Protected><!>
    typealias TestProtected6 = L<TestProtected1>

    <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>typealias TestInternal1 = Internal<!>
    <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>protected typealias TestInternal2 = Internal<!>
    internal typealias TestInternal3 = Internal
    private typealias TestInternal4 = Internal
    <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>typealias TestInternal5 = L<Internal><!>
    typealias TestInternal6 = L<TestInternal1>
}

private class Private
internal class Internal

<!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>typealias TestPrivate1 = Private<!>
<!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>internal typealias TestPrivate2 = Private<!>
private typealias TestPrivate3 = Private
<!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>typealias TestPrivate4 = L<Private><!>
typealias TestPrivate5 = L<TestPrivate1>

<!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>typealias TestInternal1 = Internal<!>
internal typealias TestInternal2 = Internal
private typealias TestInternal3 = Internal
<!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>typealias TestInternal4 = L<Internal><!>
typealias TestInternal5 = L<TestInternal1>
