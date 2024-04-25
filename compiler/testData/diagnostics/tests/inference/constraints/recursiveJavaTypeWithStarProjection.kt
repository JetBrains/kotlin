// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE

// FILE: MenuItemBase.java

public class MenuItemBase<C extends ContextMenuBase<C, I, S>, I extends MenuItemBase<C, I, S>, S extends SubMenuBase<C, I, S>> {
    public String getText() { return null; }
}

// FILE: ContextMenuBase.java

public class ContextMenuBase<C extends ContextMenuBase<C, I, S>, I extends MenuItemBase<C, I, S>, S extends SubMenuBase<C, I, S>> {}

// FILE: SubMenuBase.java

public class SubMenuBase<C extends ContextMenuBase<C, I, S>, I extends MenuItemBase<C, I, S>, S extends SubMenuBase<C, I, S>> {}

// FILE: test.kt

fun test(m: MenuItemBase<*, *, *>) {
    m.text
}