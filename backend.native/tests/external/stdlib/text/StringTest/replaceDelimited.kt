import kotlin.test.*



fun box() {
    val s = "/user/folder/file.extension"
    // chars
    assertEquals("/user/folder/file.doc", s.replaceAfter('.', "doc"))
    assertEquals("/user/folder/another.doc", s.replaceAfterLast('/', "another.doc"))
    assertEquals("new name.extension", s.replaceBefore('.', "new name"))
    assertEquals("/new/path/file.extension", s.replaceBeforeLast('/', "/new/path"))

    // strings
    assertEquals("/user/folder/file.doc", s.replaceAfter(".", "doc"))
    assertEquals("/user/folder/another.doc", s.replaceAfterLast("/", "another.doc"))
    assertEquals("new name.extension", s.replaceBefore(".", "new name"))
    assertEquals("/new/path/file.extension", s.replaceBeforeLast("/", "/new/path"))

    // non-existing delimiter
    assertEquals("/user/folder/file.extension", s.replaceAfter("=", "doc"))
    assertEquals("/user/folder/file.extension", s.replaceAfterLast("=", "another.doc"))
    assertEquals("/user/folder/file.extension", s.replaceBefore("=", "new name"))
    assertEquals("/user/folder/file.extension", s.replaceBeforeLast("=", "/new/path"))
    assertEquals("xxx", s.replaceBefore("=", "new name", "xxx"))
    assertEquals("xxx", s.replaceBeforeLast("=", "/new/path", "xxx"))
}
