// IGNORE_BACKEND: JVM



class DelegatedList : List<Int> by ArrayList()

// There's 1 line number in each of the following methods:
//   - <init>()V
//   - size()I
//   - contains(Ljava/lang/Object;)Z
//   - get(I)Ljava/lang/Object;
//   - indexOf(Ljava/lang/Object;)I
//   - lastIndexOf(Ljava/lang/Object;)I
// 6 LINENUMBER 5
