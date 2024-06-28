// FIR_IDENTICAL

// FILE: VertLikeTable.java
public interface VertLikeTable extends BasicModMajorObject, BasicModTableOrView, BasicModIdentifiedElement
// FILE: BasicModMajorObject.java
public interface BasicModMajorObject extends BasicMajorObject, BasicModSchemaObject
// FILE: BasicMajorObject.java
public interface BasicMajorObject extends BasicSchemaObject, DasSchemaChild
// FILE: BasicSchemaObject.java
public interface BasicSchemaObject extends BasicNamedElement
// FILE: BasicNamedElement.java
public interface BasicNamedElement extends BasicElement {
    @Override
    void foo();
}
// FILE: BasicElement.java
public interface BasicElement extends BasicMixinElement
// FILE: BasicMixinElement.java
public interface BasicMixinElement extends DasObject
// FILE: DasObject.java
public interface DasObject {
    default void foo() {}
}
// FILE: DasSchemaChild.java
public interface DasSchemaChild extends DasObject
// FILE: BasicModSchemaObject.java
public interface BasicModSchemaObject extends BasicSchemaObject, BasicModNamedElement
// FILE: BasicModNamedElement.java
public interface BasicModNamedElement extends BasicNamedElement, BasicModElement
// FILE: BasicModElement.java
public interface BasicModElement extends BasicElement, BasicModMixinElement
// FILE: BasicModMixinElement.java
public interface BasicModMixinElement extends BasicMixinElement
// FILE: BasicModTableOrView.java
public interface BasicModTableOrView extends BasicTableOrView, BasicModLikeTable
// FILE: BasicTableOrView.java
public interface BasicTableOrView extends BasicLikeTable, BasicMixinTableOrView
// FILE: BasicLikeTable.java
public interface BasicLikeTable extends BasicNamedElement
// FILE: BasicMixinTableOrView.java
public interface BasicMixinTableOrView extends DasTable
// FILE: DasTable.java
public interface DasTable extends DasSchemaChild
// FILE: BasicModLikeTable.java
public interface BasicModLikeTable extends BasicLikeTable, BasicModNamedElement
// FILE: BasicModIdentifiedElement.java
public interface BasicModIdentifiedElement extends BasicIdentifiedElement, BasicModElement
// FILE: BasicIdentifiedElement.java
public interface BasicIdentifiedElement extends BasicElement

// FILE: test.kt
fun foo(t: VertLikeTable) {
    t.foo()
}