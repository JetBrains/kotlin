// "Add 'const' modifier to a property" "false"
import a.Obj;
import a.A;

class B {
    void bar() {
        A a = Obj.pro<caret>perty;
        Obj.INSTANCE.getProperty();
    }
}

// ACTION: Annotate  'property' as @Deprecated
// ACTION: Add static import for 'a.Obj.property'
// ACTION: Annotate property with @JvmField
// ACTION: Split into declaration and assignment
// ACTION: Replace with getter invocation