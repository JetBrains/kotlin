module dep.intermediate {
    requires kotlin.stdlib;
    requires transitive dep.base;
    exports ipkg;
}
