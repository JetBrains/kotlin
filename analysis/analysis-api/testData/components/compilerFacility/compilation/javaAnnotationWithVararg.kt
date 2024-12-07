// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: p3/Anno.java
package p3;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE})
@Retention(RetentionPolicy.CLASS)
public @interface Anno {
    String[] value();
}

// MODULE: lib2(lib)
// MODULE_KIND: LibraryBinary
// FILE: p2/Parent.java
package p2;

import p3.Anno;

public class Parent {
    protected void onCreate() {
        setContentView(10);
    }

    public void setContentView(@Anno({"UnknownNullness", "MissingNullability"}) int id) {
    }
}

// MODULE: main(lib, lib2)
// FILE: main.kt
import p2.Parent

class Child: Parent() {
    override fun onCreate() {
        super.onCreate()
        bar()
    }

    private fun bar() {}
}
