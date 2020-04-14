class C {
    private companion object
}

typealias CAlias = C

<!EXPOSED_PROPERTY_TYPE!>val test1 = CAlias<!>
<!EXPOSED_PROPERTY_TYPE!>val test1a = C<!>