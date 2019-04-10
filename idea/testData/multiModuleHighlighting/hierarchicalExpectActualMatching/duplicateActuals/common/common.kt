package foo

// explicitly: both duplicated declaration have 'actual' modifier
// implicitly: only one duplicated declaration have 'actual' modifier, but both are matched

expect class ExplicitlyDuplicatedByMiddleAndJs
expect class ImplicitlyDuplicatedByMiddleAndJs

expect class ExplicitlyDuplicatedByMiddleAndJvm
expect class ImplicitlyDuplicatedByMiddleAndJvm