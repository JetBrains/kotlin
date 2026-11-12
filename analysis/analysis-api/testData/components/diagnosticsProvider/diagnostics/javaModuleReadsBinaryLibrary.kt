// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: module-info.java
module library {
    exports library.api;
}

// FILE: library/api/Api.java
package library.api;

public class Api {}

// MODULE: main(library)
// FILE: module-info.java
module main {
    requires library;
}

// FILE: usage.kt
fun usage() {
    library.api.Api()
}
