// FILE: PsiElement.java

public interface PsiElement {
    PsiElement[] getChildren();
}

// FILE: XmlTag.java

public interface XmlTag extends PsiElement {
    String getLocalName();
}

// FILE: test.kt

fun foo(tag: XmlTag, name: String): List<XmlTag> {
    val result = tag.children.filterIsInstance<XmlTag>().filter { it.localName == name }
    return result
}