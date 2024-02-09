// WITH_STDLIB
fun foo(libraryInfoCache: LibraryInfoCache<String, String>, outdated: List<String>) {
    val droppedLibraryInfos = libraryInfoCache.invalidateKeys(outdated).<!UNRESOLVED_REFERENCE!>flatMapTo<!>(<!CANNOT_INFER_PARAMETER_TYPE!>hashSetOf<!>()) <!UNRESOLVED_REFERENCE!>{ <!UNRESOLVED_REFERENCE!>it<!> }<!>
}

class LibraryInfoCache<Key, Value> {
    fun invalidateKeys(
        keys: Collection<Key>,
        validityCondition: ((Key, Value) -> Boolean)? = null
    ) {}
}
