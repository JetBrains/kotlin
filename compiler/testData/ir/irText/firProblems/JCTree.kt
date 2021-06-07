// TARGET_BACKEND: JVM
// FILE: JCTree.java

public class JCTree {
    public abstract static class JCExpression extends JCTree {

    }

    public static class JCTypeApply extends JCExpression {
        public String clazz = "OK";
    }
}

// FILE: JCTreeUser.kt

class Owner<out T : JCTree>(val tree: T) {
    val foo: String
        get() {
            var tree: JCTree = tree
            if (tree is JCTree.JCTypeApply) {
                return tree.clazz
            }
            return "";
        }
}