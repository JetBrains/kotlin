package test;

import static test.kotlin.KotlinEnum.ENTRY;

public class EnumStaticImportInJava {
    void other() {
        ENTRY.foo();
    }
}
