fun foo(a: Any) {
    foo(<!UNRESOLVED_REFERENCE!>{ index -> } {  }<!>)
}