abstract class A1 : CharSequence {}

abstract class A2 : CharSequence {
    override fun get(index: Int) = 'z';
}

// 2 public final bridge charAt