// !LANGUAGE: +InlineClasses

inline class Z(val x: Int)

interface IFoo
interface IBar
inline class FooBar(val x: Int) : IFoo, IBar

// 1 public final class Z extends Z\$Erased
// 1 static class Z\$Erased
// 0 public static class Z\$Erased

// 1 public final class FooBar extends FooBar\$Erased  implements IFoo IBar
// 1 static class FooBar\$Erased
// 0 public static class FooBar\$Erased