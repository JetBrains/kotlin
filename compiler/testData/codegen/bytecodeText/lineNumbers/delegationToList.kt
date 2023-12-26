// IGNORE_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-63828


class DelegatedList : List<Int> by ArrayList()

// There's 1 line number in each of the following methods:
//   - <init>()V
//   - size()I
//   - contains(Ljava/lang/Object;)Z
//   - get(I)Ljava/lang/Object;
//   - indexOf(Ljava/lang/Object;)I
//   - lastIndexOf(Ljava/lang/Object;)I
// 6 LINENUMBER 5
