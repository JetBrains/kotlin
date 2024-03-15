/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.java.decompiler.util;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.BytecodeMappingTracer;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Allows to connect text with resulting lines
 *
 * @author egor
 */
@SuppressWarnings("UnusedReturnValue")
public class TextBuffer {
  private static final boolean ALLOW_TO_STRING = Boolean.getBoolean("decompiler.allow.text.buffer.to.string");

  private final String myLineSeparator = DecompilerContext.getNewLineSeparator();
  private final String myIndent = (String)DecompilerContext.getProperty(IFernflowerPreferences.INDENT_STRING);
  private final int myPreferredLineLength = DecompilerContext.getIntOption(IFernflowerPreferences.PREFERRED_LINE_LENGTH);
  private final NewlineGroup myRootGroup = new NewlineGroup(null, 0, 0, 0);
  private NewlineGroup myCurrentGroup = myRootGroup;
  private boolean myHasReformatted = false;
  private final StringBuilder myStringBuilder;
  private Map<Integer, Integer> myLineToOffsetMapping = null;
  private final Map<BytecodeMappingKey, Integer> myBytecodeOffsetMapping = new LinkedHashMap<>(); // bytecode offset -> offset in text
  private final DebugTrace myDebugTrace = DecompilerContext.getOption(IFernflowerPreferences.UNIT_TEST_MODE) ? new DebugTrace(this) : null;

  public TextBuffer() {
    myStringBuilder = new StringBuilder();
  }

  public TextBuffer(int size) {
    myStringBuilder = new StringBuilder(size);
  }

  public TextBuffer(String text) {
    myStringBuilder = new StringBuilder(text);
  }

  public TextBuffer append(String str) {
    myStringBuilder.append(str);
    return this;
  }

  public TextBuffer append(char ch) {
    myStringBuilder.append(ch);
    return this;
  }

  public TextBuffer append(int i) {
    myStringBuilder.append(i);
    return this;
  }

  public TextBuffer appendLineSeparator() {
    myStringBuilder.append(myLineSeparator);
    return this;
  }

  public TextBuffer appendIndent(int length) {
    while (length-- > 0) {
      append(myIndent);
    }
    return this;
  }

  /**
   * Pushes a newline group.
   * <p>
   * {@link #appendPossibleNewline} normally works like append, adding its string argument to the buffer. However, if
   * the line is running out of space, this text may later be replaced with a newline (plus indents).
   * Possible newlines are grouped into newline groups. In a newline group, either none of the replacements are replaced
   * with newlines, or all of them are (never only some of them). Possible newlines should not be added outside a group.
   * The API works as if there were a stack of newline groups. This method pushes to the stack and
   * {@link #popNewlineGroup()} pops from it. After the full source code has been generated, {@link #reformat()} is
   * called to actually substitute the text.
   *
   * @param baseIndent The indent that has already been explicitly applied
   * @param extraIndent The indent to be added if this group is applied
   */
  public TextBuffer pushNewlineGroup(int baseIndent, int extraIndent) {
    NewlineGroup group = new NewlineGroup(myCurrentGroup, myStringBuilder.length(), baseIndent, extraIndent);
    myCurrentGroup.myChildren.add(group);
    myCurrentGroup = group;
    return this;
  }

  /**
   * If the current group is applied, add a newline here.
   */
  public TextBuffer appendPossibleNewline() {
    return appendPossibleNewline("");
  }

  /**
   * If the current group is applied, add a newline here. Otherwise, add the argument.
   */
  public TextBuffer appendPossibleNewline(String alternative) {
    return appendPossibleNewline(alternative, false);
  }

  /**
   * If the current group is applied, add a newline here. Otherwise, add the argument.
   * If {@code dedent} is true, the extra indent from this group is unapplied from this point on.
   */
  public TextBuffer appendPossibleNewline(String alternative, boolean dedent) {
    myCurrentGroup.myReplacements.add(new NewlineGroup.Replacement(myStringBuilder.length(), alternative.length(), dedent));
    return append(alternative);
  }

  public TextBuffer popNewlineGroup() {
    if (myCurrentGroup == myRootGroup) {
      throw new IllegalStateException("Cannot pop root group: " + this.convertToStringAndAllowDataDiscard());
    }
    assert myStringBuilder.length() >= myCurrentGroup.myStart;
    myCurrentGroup.myLength = myStringBuilder.length() - myCurrentGroup.myStart;
    myCurrentGroup = myCurrentGroup.myParent;
    return this;
  }

  public TextBuffer prepend(String s) {
    myStringBuilder.insert(0, s);
    shiftMapping(s.length());
    return this;
  }

  public TextBuffer enclose(String left, String right) {
    prepend(left);
    append(right);
    return this;
  }

  public TextBuffer encloseWithParens() {
    return enclose("(", ")");
  }

  public boolean containsOnlyWhitespaces() {
    for (int i = 0; i < myStringBuilder.length(); i++) {
      if (myStringBuilder.charAt(i) != ' ') {
        return false;
      }
    }
    return true;
  }

  public void addBytecodeMapping(int bytecodeOffset) {
    if (myDebugTrace != null) {
      myDebugTrace.myPreventDeletion = true;
    }
    myBytecodeOffsetMapping.putIfAbsent(new BytecodeMappingKey(bytecodeOffset, null, null), myStringBuilder.length());
  }

  public void addStartBytecodeMapping(int bytecodeOffset) {
    if (myDebugTrace != null) {
      myDebugTrace.myPreventDeletion = true;
    }
    myBytecodeOffsetMapping.putIfAbsent(new BytecodeMappingKey(bytecodeOffset, null, null), 0);
  }

  public void addBytecodeMapping(BitSet bytecodeOffsets) {
    if (bytecodeOffsets == null) {
      return;
    }
    for (int i = bytecodeOffsets.nextSetBit(0); i >= 0; i = bytecodeOffsets.nextSetBit(i + 1)) {
      addBytecodeMapping(i);
    }
  }

  public void addStartBytecodeMapping(BitSet bytecodeOffsets) {
    if (bytecodeOffsets == null) {
      return;
    }
    for (int i = bytecodeOffsets.nextSetBit(0); i >= 0; i = bytecodeOffsets.nextSetBit(i + 1)) {
      addStartBytecodeMapping(i);
    }
  }

  public void clearUnassignedBytecodeMappingData() {
    myBytecodeOffsetMapping.keySet().removeIf(key -> key.myClass == null);
  }

  public Map<Pair<String, String>, BytecodeMappingTracer> getTracers() {
    List<Integer> newlineOffsets = new ArrayList<>();
    for (int i = myStringBuilder.indexOf(myLineSeparator); i != -1; i = myStringBuilder.indexOf(myLineSeparator, i + 1)) {
      newlineOffsets.add(i);
    }
    Map<Pair<String, String>, BytecodeMappingTracer> tracers = new LinkedHashMap<>();
    myBytecodeOffsetMapping.forEach((key, textOffset) -> {
      if (key.myClass == null) {
        throw new IllegalStateException("getTracers called when not all bytecode offsets have a valid class and method");
      }
      BytecodeMappingTracer tracer = tracers.computeIfAbsent(Pair.of(key.myClass, key.myMethod), k -> new BytecodeMappingTracer());
      int lineNo = Collections.binarySearch(newlineOffsets, textOffset);
      if (lineNo < 0) {
        lineNo = -lineNo - 1;
      }
      tracer.setCurrentSourceLine(lineNo);
      tracer.addMapping(key.myBytecodeOffset);
    });
    return tracers;
  }

  /**
   * Recursive reformatting of groups.
   * If the group's content would take you over the specified preferred line length, then the group is applied.
   *
   * - This algorithm usually ensures that if a group is applied, then all the group's parents in the tree are also
   *   applied. This tends to produce a nice structured output with well-placed potential newlines.
   * - The formatter keeps track of the offset as a result of the reformat at each character in the text buffer, and
   *   applies them to the bytecode offsets afterwards, so that the line number mappings don't get messed up.
   */
  private void reformatGroup(NewlineGroup group, List<Integer> offsetMapping, int extraIndent) {
    int offset = offsetMapping.get(group.myStart);
    int actualStart = group.myStart + offset;
    // Find the last newline before the start of this group, so we know how long the line already is
    int lastNewline = myStringBuilder.lastIndexOf(myLineSeparator, actualStart);
    int nextNewline = myStringBuilder.indexOf(myLineSeparator, actualStart);
    // Find the end of the first line of this group, if the group were to not be reformatted
    int firstPartEnd = nextNewline == -1 ? actualStart + group.myLength : Math.min(nextNewline, actualStart + group.myLength);
    // Go through all lines in the group, and find the longest one (without reformatting)
    int groupEndWithoutNewlines = lastNewline == -1 ? firstPartEnd : firstPartEnd - lastNewline;
    while (nextNewline != -1 && nextNewline <= actualStart + group.myLength) {
      int lineStart = nextNewline;
      int lineEnd = nextNewline = myStringBuilder.indexOf(myLineSeparator, nextNewline + 1);
      if (lineEnd == -1 || lineEnd > actualStart + group.myLength) {
        lineEnd = actualStart + group.myLength;
      }
      int lineLength = extraIndent + lineEnd - lineStart - myLineSeparator.length();
      if (lineLength > groupEndWithoutNewlines) {
        groupEndWithoutNewlines = lineLength;
      }
    }
    // If the longest line is longer than the preferred line length, then reformat the group
    boolean addNewLines = groupEndWithoutNewlines > myPreferredLineLength;

    int originalExtraIndent = extraIndent;
    // If we're reformatting this group, and not only its children, then apply extra indentation
    if (addNewLines && !group.myReplacements.isEmpty()) {
      extraIndent += group.myExtraIndent;
    }

    // Iterate over the children and replacements in one go rather than separately, because offsets need to be applied
    // from left to right
    int childrenIndex = 0;
    int replacementIndex = 0;
    for (int pos = group.myStart; pos <= group.myStart + group.myLength; pos++) {
      if (pos != group.myStart) {
        offsetMapping.add(offset);
      }
      assert offsetMapping.size() == pos + 1;

      // add extra indent after newlines
      if (pos + offset + myLineSeparator.length() < myStringBuilder.length() && myStringBuilder.substring(pos + offset, pos + offset + myLineSeparator.length()).equals(myLineSeparator)) {
        for (int i = 0; i < extraIndent; i++) {
          myStringBuilder.insert(pos + offset + myLineSeparator.length(), myIndent);
        }
        offset += myIndent.length() * extraIndent;
      }

      // do multiple passes in an inner loop, as there could be arbitrarily many with the same offset
      // or replacements with offsets equal to the end position of a child group
      boolean anotherPass = true;
      while (anotherPass) {
        anotherPass = false;

        // replace replaceables with newlines
        if (addNewLines && replacementIndex < group.myReplacements.size() && pos == group.myReplacements.get(replacementIndex).myStart) {
          NewlineGroup.Replacement replacement = group.myReplacements.get(replacementIndex);
          myStringBuilder.replace(pos + offset, pos + offset + replacement.myLength, myLineSeparator);
          if (replacement.myDedent) {
            extraIndent = originalExtraIndent;
          }
          for (int i = 0; i < group.myBaseIndent + extraIndent; i++) {
            myStringBuilder.insert(pos + offset + myLineSeparator.length(), myIndent);
          }
          offset += myIndent.length() * (group.myBaseIndent + extraIndent) + myLineSeparator.length() - replacement.myLength;
          replacementIndex++;
          anotherPass = true;
        }
        // offset may have changed, update the offset mapping here because the child groups rely on the value being
        // correct in the list
        offsetMapping.set(offsetMapping.size() - 1, offset);

        // recursively iterate through child groups
        int currentPos = pos;
        if (childrenIndex < group.myChildren.size() && group.myChildren.get(childrenIndex).myStart == currentPos) {
          NewlineGroup child = group.myChildren.get(childrenIndex);
          reformatGroup(child, offsetMapping, extraIndent);
          offset = offsetMapping.get(offsetMapping.size() - 1);
          pos += child.myLength;
          childrenIndex++;
          anotherPass = true;
        }
      }
    }

    // update the offset value here because it might have changed when adding indents after existing newlines
    // the parent relies on the value being correct in the list
    offsetMapping.set(offsetMapping.size() - 1, offset);
  }


  public void reformat() {
    if (myCurrentGroup != myRootGroup) {
      throw new IllegalStateException("Cannot reformat while in a group");
    }
    if (myHasReformatted) {
      throw new IllegalStateException("Cannot reformat twice");
    }
    myHasReformatted = true;

    //myRootGroup.dump("");

    myRootGroup.myLength = myStringBuilder.length();

    List<Integer> offsetMapping = new ArrayList<>(myStringBuilder.length());
    offsetMapping.add(0);
    reformatGroup(myRootGroup, offsetMapping, 0);

    myBytecodeOffsetMapping.replaceAll((key, value) -> value + offsetMapping.get(value));
  }

  public boolean contentEquals(String string) {
    return myStringBuilder.toString().equals(string);
  }

  public String convertToStringAndAllowDataDiscard() {
    if (myDebugTrace != null) {
      myDebugTrace.myPreventDeletion = false;
    }
    String original = myStringBuilder.toString();
    if (myLineToOffsetMapping == null || myLineToOffsetMapping.isEmpty()) {
      if (myLineMapping != null) {
        return addOriginalLineNumbers();
      }
      return original;
    }
    else {
      StringBuilder res = new StringBuilder();
      String[] srcLines = original.split(myLineSeparator);
      int currentLineStartOffset = 0;
      int currentLine = 0;
      int previousMarkLine = 0;
      int dumpedLines = 0;
      ArrayList<Integer> linesWithMarks = new ArrayList<>(myLineToOffsetMapping.keySet());
      Collections.sort(linesWithMarks);
      for (Integer markLine : linesWithMarks) {
        Integer markOffset = myLineToOffsetMapping.get(markLine);
        while (currentLine < srcLines.length) {
          String line = srcLines[currentLine];
          int lineEnd = currentLineStartOffset + line.length() + myLineSeparator.length();
          if (markOffset <= lineEnd) {
            int requiredLine = markLine - 1;
            int linesToAdd = requiredLine - dumpedLines;
            dumpedLines = requiredLine;
            appendLines(res, srcLines, previousMarkLine, currentLine, linesToAdd);
            previousMarkLine = currentLine;
            break;
          }
          currentLineStartOffset = lineEnd;
          currentLine++;
        }
      }
      if (previousMarkLine < srcLines.length) {
        appendLines(res, srcLines, previousMarkLine, srcLines.length, srcLines.length - previousMarkLine);
      }

      return res.toString();
    }
  }

  @Override
  public String toString() {
    if (!ALLOW_TO_STRING) {
      if (DecompilerContext.getOption(IFernflowerPreferences.UNIT_TEST_MODE)) {
        throw new AssertionError("Usage of TextBuffer.toString");
      } else {
        DecompilerContext.getLogger().writeMessage("Usage of TextBuffer.toString", IFernflowerLogger.Severity.WARN);
      }
    }
    return convertToStringAndAllowDataDiscard();
  }

  private String addOriginalLineNumbers() {
    StringBuilder sb = new StringBuilder();
    int lineStart = 0, lineEnd;
    int count = 0, length = myLineSeparator.length();
    while ((lineEnd = myStringBuilder.indexOf(myLineSeparator, lineStart)) > 0) {
      ++count;
      sb.append(myStringBuilder.substring(lineStart, lineEnd));
      Set<Integer> integers = myLineMapping.get(count);
      if (integers != null) {
        sb.append("//");
        for (Integer integer : integers) {
          sb.append(' ').append(integer);
        }
      }
      sb.append(myLineSeparator);
      lineStart = lineEnd + length;
    }
    if (lineStart < myStringBuilder.length()) {
      sb.append(myStringBuilder.substring(lineStart));
    }
    return sb.toString();
  }

  private void appendLines(StringBuilder res, String[] srcLines, int from, int to, int requiredLineNumber) {
    if (to - from > requiredLineNumber) {
      List<String> strings = compactLines(Arrays.asList(srcLines).subList(from, to) ,requiredLineNumber);
      int separatorsRequired = requiredLineNumber - 1;
      for (String s : strings) {
        res.append(s);
        if (separatorsRequired-- > 0) {
          res.append(myLineSeparator);
        }
      }
      res.append(myLineSeparator);
    }
    else if (to - from <= requiredLineNumber) {
      for (int i = from; i < to; i++) {
        res.append(srcLines[i]).append(myLineSeparator);
      }
      for (int i = 0; i < requiredLineNumber - to + from; i++) {
        res.append(myLineSeparator);
      }
    }
  }

  public int length() {
    return myStringBuilder.length();
  }

  public void setStart(int position) {
    myStringBuilder.delete(0, position);
    shiftMapping(-position);
  }

  public void setLength(int position) {
    myStringBuilder.setLength(position);
    if (myLineToOffsetMapping != null) {
      Map<Integer, Integer> newMap = new HashMap<>();
      for (Map.Entry<Integer, Integer> entry : myLineToOffsetMapping.entrySet()) {
        if (entry.getValue() <= position) {
          newMap.put(entry.getKey(), entry.getValue());
        }
      }
      myLineToOffsetMapping = newMap;
    }
    myRootGroup.truncate(position);
    assert currentGroupExists();
  }

  private boolean currentGroupExists() {
    for (NewlineGroup group = myCurrentGroup; group != myRootGroup; group = group.myParent) {
      if (!group.myParent.myChildren.contains(group)) {
        return false;
      }
    }
    return true;
  }

  public TextBuffer append(TextBuffer buffer, String className, String methodKey) {
    if (buffer.myCurrentGroup != buffer.myRootGroup) {
      throw new IllegalArgumentException("Can't append buffer with non-root group");
    }
    if (buffer.myDebugTrace != null) {
      buffer.myDebugTrace.myPreventDeletion = false;
    }
    if (buffer.myLineToOffsetMapping != null && !buffer.myLineToOffsetMapping.isEmpty()) {
      checkMapCreated();
      for (Map.Entry<Integer, Integer> entry : buffer.myLineToOffsetMapping.entrySet()) {
        myLineToOffsetMapping.put(entry.getKey(), entry.getValue() + myStringBuilder.length());
      }
    }
    buffer.myBytecodeOffsetMapping.forEach((key, value) -> {
      if (key.myClass == null) {
        key = new BytecodeMappingKey(key.myBytecodeOffset, className, methodKey);
      }
      myBytecodeOffsetMapping.putIfAbsent(key, value + myStringBuilder.length());
    });
    NewlineGroup otherRoot = buffer.myRootGroup.copy();
    otherRoot.shift(myStringBuilder.length());
    myCurrentGroup.myReplacements.addAll(otherRoot.myReplacements);
    myCurrentGroup.myChildren.addAll(otherRoot.myChildren);
    myStringBuilder.append(buffer.myStringBuilder);
    return this;
  }

  public TextBuffer append(TextBuffer buffer) {
    return append(buffer, null, null);
  }

  private void shiftMapping(int shiftOffset) {
    if (myLineToOffsetMapping != null) {
      Map<Integer, Integer> newMap = new HashMap<>();
      for (Map.Entry<Integer, Integer> entry : myLineToOffsetMapping.entrySet()) {
        int newValue = entry.getValue();
        if (newValue >= 0) {
          newValue += shiftOffset;
        }
        if (newValue >= 0) {
          newMap.put(entry.getKey(), newValue);
        }
      }
      myLineToOffsetMapping = newMap;
    }
    myBytecodeOffsetMapping.replaceAll((key, value) -> value + shiftOffset);
    myRootGroup.shift(shiftOffset);
  }

  private void checkMapCreated() {
    if (myLineToOffsetMapping == null) {
      myLineToOffsetMapping = new HashMap<>();
    }
  }

  public int countLines() {
    return countLines(0);
  }

  public int countLines(int from) {
    return count(myLineSeparator, from);
  }

  public int count(String substring, int from) {
    int count = 0, length = substring.length(), p = from;
    while ((p = myStringBuilder.indexOf(substring, p)) > 0) {
      ++count;
      p += length;
    }
    return count;
  }

  @Deprecated
  public int countChars(char c) {
    convertToStringAndAllowDataDiscard();

    int count = 0;

    CharSequence chars = myStringBuilder.subSequence(0, myStringBuilder.length());
    for (int i = 0; i < chars.length(); i++) {
      if (chars.charAt(i) == c) {
        count++;
      }
    }

    return count;
  }

  private static List<String> compactLines(List<String> srcLines, int requiredLineNumber) {
    if (srcLines.size() < 2 || srcLines.size() <= requiredLineNumber) {
      return srcLines;
    }
    List<String> res = new LinkedList<>(srcLines);
    // first join lines with a single { or }
    for (int i = res.size()-1; i > 0 ; i--) {
      String s = res.get(i);
      if (s.trim().equals("{") || s.trim().equals("}")) {
        res.set(i-1, res.get(i-1).concat(s));
        res.remove(i);
      }
      if (res.size() <= requiredLineNumber) {
        return res;
      }
    }
    // now join empty lines
    for (int i = res.size()-1; i > 0 ; i--) {
      String s = res.get(i);
      if (s.trim().isEmpty()) {
        res.set(i-1, res.get(i-1).concat(s));
        res.remove(i);
      }
      if (res.size() <= requiredLineNumber) {
        return res;
      }
    }
    return res;
  }

  private Map<Integer, Set<Integer>> myLineMapping = null; // new to original

  public void dumpOriginalLineNumbers(int[] lineMapping) {
    if (lineMapping.length > 0) {
      myLineMapping = new HashMap<>();
      for (int i = 0; i < lineMapping.length; i += 2) {
        int key = lineMapping[i + 1];
        Set<Integer> existing = myLineMapping.computeIfAbsent(key, k -> new TreeSet<>());
        existing.add(lineMapping[i]);
      }
    }
  }

  private static final class BytecodeMappingKey {
    private final int myBytecodeOffset;
    // null signifies the current class
    private final String myClass;
    private final String myMethod;

    public BytecodeMappingKey(int bytecodeOffset, String className, String methodKey) {
      myBytecodeOffset = bytecodeOffset;
      myClass = className;
      myMethod = methodKey;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      BytecodeMappingKey that = (BytecodeMappingKey)o;
      return myBytecodeOffset == that.myBytecodeOffset &&
             Objects.equals(myClass, that.myClass) &&
             Objects.equals(myMethod, that.myMethod);
    }

    @Override
    public int hashCode() {
      return Objects.hash(myBytecodeOffset, myClass, myMethod);
    }

    @Override
    public String toString() {
      return myClass + ":" + myMethod + ":" + myBytecodeOffset;
    }
  }

  private static final class NewlineGroup {
    final NewlineGroup myParent;
    int myStart;
    int myLength;
    final int myBaseIndent;
    final int myExtraIndent;
    final List<NewlineGroup> myChildren = new ArrayList<>();
    final List<Replacement> myReplacements = new ArrayList<>();

    NewlineGroup(NewlineGroup parent, int start, int baseIndent, int extraIndent) {
      this.myParent = parent;
      this.myStart = start;
      this.myBaseIndent = baseIndent;
      this.myExtraIndent = extraIndent;
    }

    void shift(int amount) {
      myStart += amount;
      for (Replacement replacement : myReplacements) {
        replacement.myStart += amount;
      }
      for (NewlineGroup child : myChildren) {
        child.shift(amount);
      }
    }

    void truncate(int stringLength) {
      if (myStart + myLength > stringLength) {
        myLength = stringLength - myStart;
      }
      for (Iterator<NewlineGroup> itr = myChildren.iterator(); itr.hasNext(); ) {
        NewlineGroup child = itr.next();
        if (child.myStart <= stringLength) {
          child.truncate(stringLength);
        } else {
          itr.remove();
        }
      }
      myReplacements.removeIf(r -> r.myStart > stringLength);
    }

    void dump(String indent) {
      System.out.println(indent + "group " + myStart + "-" + (myStart + myLength) + ": " + myReplacements.size() + " replacements");
      for (NewlineGroup child : myChildren) {
        child.dump(indent + "  ");
      }
    }

    NewlineGroup copy() {
      NewlineGroup copy = new NewlineGroup(myParent, myStart, myBaseIndent, myExtraIndent);
      copy.myLength = myLength;
      for (NewlineGroup child : myChildren) {
        copy.myChildren.add(child.copy());
      }
      copy.myReplacements.addAll(myReplacements);
      return copy;
    }

    private static class Replacement {
      int myStart;
      final int myLength;
      final boolean myDedent;

      Replacement(int start, int length, boolean dedent) {
        this.myStart = start;
        this.myLength = length;
        this.myDedent = dedent;
      }
    }
  }

  public static void checkLeaks() {
    DebugTrace.checkLeaks();
  }

  // it's really important that this class does not directly or indirectly reference the TextBuffer, or we will create memory leaks
  static class DebugTrace extends WeakReference<TextBuffer> {
    private static final Set<DebugTrace> ALL_REMAINING_TRACES = ConcurrentHashMap.newKeySet();

    private static final AtomicBoolean STARTED = new AtomicBoolean();
    private static final ReferenceQueue<TextBuffer> REFERENCE_QUEUE = new ReferenceQueue<>();

    private static void ensureStarted() {
      if (!STARTED.getAndSet(true)) {
        Thread cleaner = new Thread(() -> {
          while (true) {
            DebugTrace trace;
            try {
              trace = (DebugTrace) REFERENCE_QUEUE.remove();
            } catch (InterruptedException e) {
              break;
            }
            trace.onDeletion();
            ALL_REMAINING_TRACES.remove(trace);
          }
        });
        cleaner.setName("TextBuffer debug cleaner");
        cleaner.setDaemon(true);
        cleaner.start();
      }
    }

    DebugTrace(TextBuffer buffer) {
      super(buffer, REFERENCE_QUEUE);
      ensureStarted();
      ALL_REMAINING_TRACES.add(this);
    }

    final Throwable myCreationTrace = new Throwable();
    boolean myPreventDeletion = false;

    private void onDeletion() {
      if (myPreventDeletion && (!DotExporter.DUMP_DOTS && !DotExporter.DUMP_ERROR_DOTS)) {
        throw new AssertionError(
          "TextBuffer was garbage collected without being added to another TextBuffer, data loss occurred. See cause for the creation trace",
          myCreationTrace
        );
      }
    }

    static void checkLeaks() {
      for (DebugTrace trace : ALL_REMAINING_TRACES) {
        trace.onDeletion();
      }
      ALL_REMAINING_TRACES.clear();
    }
  }
}