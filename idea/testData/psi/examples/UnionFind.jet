class UnionFind {
  private val data = IMutableList<Int>()

  fun add() : Int {
    val size = data.size
    data.add(size)
    size
  }

  private fun parent(x : Int) : Int {
    val p = data[x];
    if (p == x) {
      return x;
    }
    val result = parent(p);
    data[x] = result;
  }

  fun union(a : Int, b : Int) {
    val pa = parent(a)
    val pb = parent(b)
    if (pa != pb) {
      if (Random.nextInt().isOdd) {
        data[pb] = pa
      } else {
        data[pa] = pb
      }
    }
  }
}

val Int.isOdd : Boolean
  get() = this % 2 != 0
