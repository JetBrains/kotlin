class Vertex<V>(val data : V)

class Edge<V, E>(val from : V, val data : E, val to : V)

class Graph<V, E> {

  private val mutableEdges = new ArrayList<Edge<V, E>>() // type is ArrayList, but I want IMutableList
/* options:
    private val edges : IMutableList<Edge<V, E>> = new ArrayList<Edge<V, E>>()
    private val edges : IMutableList<Edge<V, E>> = new ArrayList() // not an erasure, but a request to infer parameters
*/

  private val mutableVertices = new HashSet<Vertex<V>>()

  val edges : IList<Edge<V, E>> = mutableEdges;
  val vertices : ISet<Edge<V, E>> = mutableVertices;

  fun addEdge(from : V, data : E, to : V) {
    mutableEdges.add(new Edge(from, data, to)) // constructor parameters are inferred
  }
  fun addVertex(v : V) {
    mutableEdges.add(new Edge(from, data, to)) // constructor parameters are inferred
  }

  fun neighbours(v : Vertex<V>) = edges.filter{it.from == v}.map{it.to} // type is IIterable<Vertex<V>>

  fun dfs(handler : {(V) : Unit}) {
    val visited = new HashSet<Vertex<V>>()
    vertices.foreach{dfs(it, visited, handler)}

    fun dfs(current : Vertex<V>, visited : ISet<Vertex<V>>, handler : {(V) : Unit}) {
      if (!visited.add(current))
        return
      handler(current)
      neighbours(current).foreach{dfs(it, visited, handler)}
    }
  }

  public fun traverse(pending : IPushPop<Vertex<V>>, visited : ISet<Vertex<V>>, handler : {(V) : Unit}) {
    vertices.foreach {
      if (!visited.add(it))
        continue
      pending.push(it)
      while (!pending.isEmpty) {
        val current = pending.pop()
        handler(current);
        neighbours(current).foreach { n =>
          if (visited.add(n)) {
            pending.push(n)
          }
        }
    /* alternative
        pending->push(neighbours(current).filter{n => !visited[n])})
        // -> means that if push(x : T) and actual parameter y is IIterable<T>, this compiles into
          y.foreach{ n => push(n) }
     */
      }
    }
  }
}
