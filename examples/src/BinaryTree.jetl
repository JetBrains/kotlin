class BinaryTree<T> : IMutableSet<T> {
  private class TreeNode(
    var value : T, var parent : TreeNode
  ) {
    var left : TreeNode
    var right : TreeNode

  }

  private val compare : MatchableComparison<T>
  private var root : TreeNode
  private var version = 0

//  override var size : Int { get; private set; }

  this(compare : Comparison<T>) {
    this.compare = asMatchableComparison(comparison)
  }

  this() : this(naturalOrder<T>()) {
  }

  private [operator] fun T.compareTo(other : T) : Int = compare(this, other)

  override fun contains(item : T) : Boolean {
    return contains(root, item)

    fun contains(node : TreeNode, item : T) : Boolean {
      if (node == null) return false
      when (compare(item, node.value)) {
        EQ => true
        LS => contains(node.left, item)
        GT => contains(node.right, item)
      }
    }
  }

  override fun add(item : T) : Boolean {
    if (add(ref root, null)) {
      size++
      version++
      return true
    }
    return false

    // In principle, this has access to item anyway, but then it's unreachable code
    // BAD: the naive implementation of ref will create H(T) ref objects, but can be optimized to create only one
    fun add(ref node : TreeNode, parent : TreeNode) : Boolean {
      if (node == null) {
        node = TreeNode(item, parent)
        return true
      }
      when (compare(item, node.value)) {
        EQ => false
        LS => add(ref node.left, node)
        GT => add(ref node.right, node)
      }
    }

    // In principle, this has access to item anyway
    fun addNoRef(node : TreeNode) : Boolean {
      if (node == null) {
        root = TreeNode(item, null)
        return true
      }
      when (compare(item, node.value)) {
        EQ => return false
        LS =>
          if (node.left == null) {
            node.left = TreeNode(item, node)
            return true
          } else return add(node.left)
        GT =>
          if (node.right == null) {
            node.right = TreeNode(item, node)
            return true
          } else return add(node.right)
      }
    }
  }

  override fun remove(item : T) : Boolean {
    val toRemove = find(root, item)
    if (toRemove == null) return false
    remove(toRemove)
    size--
    version++
    return true

    fun find(node : TreeNode) : TreeNode {
      if (node == null) return null
      when (compare(item, node.value)) {
        EQ => node
        LS => find(node.left)
        GT => find(node.right)
      }
    }
  }

  private fun remove(node : TreeNode) {
    when (node) {
      is TreeNode(null, null) => replace(node, null)
      is TreeNode(null, ?right) => replace(node, right)
      is TreeNode(?left, null) => replace(node, left)
      is TreeNode(?left, ?right) => {
        val min = min(node.right)
        node.value = min.value
        remove(min)
      }
    }

    fun replace(node : TreeNode, replace : TreeNode) {
      if (node == root) {
        root = replace
        return
      }
      if (node.parent.left == node) {
        node.parent.left = replace
      } else if (node.parent.right == node) {
        node.parent.right = replace
      } else Assert(false)
    }
  }

  // Relies on tail-recursion optimization
  private fun min(node : TreeNode) {
    if (node.left == null) node else min(node.left)
  }

  override fun iterator() : IIterator<T> = mutableIterator()

  override fun mutableIterator() : IMutableIterator<T> = object IMutableIterator {
    val version = BinaryTree.this.version
    val down = Stack<TreeNode>()
    val up = Stack<TreeNode>()
    var lastNode : TreeNode

    this() {
      if (root != null)
        down.push(root)    
    }

    override fun next() : T {
      if (!hasNext)
        throw NoMoreElementsException()
      checkVersion()

      lastNode = nextNode()
      return lastNode.value
    }

    private fun nextNode() : TreeNode {
      while (true) {
        if (!down.isEmpty) {
          val curNode = down.pop()
          if (curNode.left != null) {
            up.push(curNode)
            down.push(curNode.left)
          } else {
            if (curNode.right != null) {
              down.push(curNode.right)
            }
            return curNode;
          }
        } else {
          val curNode = up.pop()
          if (curNode.right != null) {
            down.push(curNode.right)
          }
          return curNode
        }
      }
    }

    override val hasNext : Boolean
      get() = !down.isEmpty || !up.isEmpty


    override fun remove() {
      checkVersion()
      if (lastNode == null)
        throw IterationException("Nothing to remove")
      remove(lastNode)
      version++
      BinaryTree.this.version = version
    }

    private fun checkVersion() {
      if (version != BinaryTree.this.version) {
        throw ConcurrentModificationException()
      }                                              
    }
  }

}