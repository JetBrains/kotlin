// ISSUE: KT-42449

data class NodePropertyDescriptor<TNode : Node, TProperty : Any, TPropertyVal : TProperty?>(
    val description: String,
    val propertyRef: NodePropertyRef<TNode, TProperty, TPropertyVal>,
) {
    fun test_1(other: Any?): Boolean {
        if (other !is NodePropertyDescriptor<*, *, *>) return false
        if (description != other.description) return false
        if (propertyRef != other.propertyRef) return false
        return true
    }

    fun test_2(other: Any?): Boolean {
        if (other !is NodePropertyDescriptor<*, *, *>) return false
        if (<!USELESS_IS_CHECK!>other is NodePropertyDescriptor<*, *, *><!>) {
            if (description != other.description) return false
            if (propertyRef != other.propertyRef) return false
        }
        return true
    }

    fun test_3(other: Any?): Boolean {
        if (other is NodePropertyDescriptor<*, *, *>) {
            if (description != other.description) return false
            if (propertyRef != other.propertyRef) return false
        }
        return true
    }

}

class NodePropertyRef<T, U, V>

open class Node
