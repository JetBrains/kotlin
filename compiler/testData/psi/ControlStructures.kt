
fun a(
    a : foo = throw Foo(),
    a : foo = return 10,
    a : foo = break,
    a : foo = break@la,
    a : foo = continue,
    a : foo = continue@la,
    a : foo = if (10) foo else bar,
    a : foo = if (10) foo
) {
  return 10
  return
  10
  break
  la@
  break@la
  continue
  la@
  continue@la
  if (foo)
    if (foo)
      bar
    else
      foo
  else if (foo)
    bar
  else
    bar

  try {

  }
  catch (Foo : Bar) {

  }
  try {

  }
  catch (Foo : Bar) {

  }
  catch (Foo : Bar) {

  }
  catch (Foo : Bar) {

  }
  try {

  }
  catch (Foo : Bar) {

  }
  catch (Foo : Bar) {

  }
  finally {

  }
  try {

  }
  finally {

  }

  for (val x in foo) a
    for (x in foo) a
      for (val x : Int in foo) a
        for (x : Int in foo) {}

  while (true) {}

  do {

  } while (false)
}

fun foo() {
    for (a in b)
      b

    for (a in b) {}

    for (a in b) {
      b
    }

    for (a in b);
      b

    while (a in b)
      b

    while (a in b) {
      b
    }

    while (a in b);
      b

    while (a) {}

    if (a)
      b
    else
      c

    if (a) b else c

    if (a) b
    else c

    if (a)
      b;
    else
      c;

    if (a) b
    if (a)
      b
    if (a)
      b;

    if (a) else c
    if (a)
    else c
    if (a)
     ;
    else c
    if (a)
    else ;

    do while (r)
    do foo while (r)
    do {;;;foo;bar;;;; } while (r)
}