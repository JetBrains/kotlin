val x = if (flag) {
    if (flag2) 1<caret>3 else 7
} else 42

// TYPE: if (flag2) 13 else 7 -> <html>Int</html>
