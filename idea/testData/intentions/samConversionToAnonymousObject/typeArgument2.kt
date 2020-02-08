// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

val o = <caret>OwnAble { x: Int, y: Long -> "$x $y" }
