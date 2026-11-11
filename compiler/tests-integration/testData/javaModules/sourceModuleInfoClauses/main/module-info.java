module main {
    requires static lib;
    requires transitive lib2;

    requires kotlin.stdlib;

    exports main.api to some.other.module, /* a comment */ another.module;
}
