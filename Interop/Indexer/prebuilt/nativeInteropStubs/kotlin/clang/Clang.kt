package clang

import kotlin_native.interop.*

fun asctime(arg0: tm?): Int8Box? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.asctime(_arg0)
    return NativePtr.byValue(res).asRef(Int8Box)
}

fun clock(): Long {
    val res = externals.clock()
    return res
}

fun ctime(arg0: Int64Box?): Int8Box? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.ctime(_arg0)
    return NativePtr.byValue(res).asRef(Int8Box)
}

fun getdate(arg0: String?): tm? {
    val _arg0 = CString.fromString(arg0).getNativePtr().asLong()
    val res = externals.getdate(_arg0)
    free(NativePtr.byValue(_arg0))
    return NativePtr.byValue(res).asRef(tm)
}

fun gmtime(arg0: Int64Box?): tm? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.gmtime(_arg0)
    return NativePtr.byValue(res).asRef(tm)
}

fun localtime(arg0: Int64Box?): tm? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.localtime(_arg0)
    return NativePtr.byValue(res).asRef(tm)
}

fun mktime(arg0: tm?): Long {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.mktime(_arg0)
    return res
}

fun strftime(arg0: String?, arg1: Long, arg2: String?, arg3: tm?): Long {
    val _arg0 = CString.fromString(arg0).getNativePtr().asLong()
    val _arg2 = CString.fromString(arg2).getNativePtr().asLong()
    val _arg3 = arg3.getNativePtr().asLong()
    val res = externals.strftime(_arg0, arg1, _arg2, _arg3)
    free(NativePtr.byValue(_arg0))
    free(NativePtr.byValue(_arg2))
    return res
}

fun strptime(arg0: String?, arg1: String?, arg2: tm?): Int8Box? {
    val _arg0 = CString.fromString(arg0).getNativePtr().asLong()
    val _arg1 = CString.fromString(arg1).getNativePtr().asLong()
    val _arg2 = arg2.getNativePtr().asLong()
    val res = externals.strptime(_arg0, _arg1, _arg2)
    free(NativePtr.byValue(_arg0))
    free(NativePtr.byValue(_arg1))
    return NativePtr.byValue(res).asRef(Int8Box)
}

fun time(arg0: Int64Box?): Long {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.time(_arg0)
    return res
}

fun tzset(): Unit {
    val res = externals.tzset()
    return res
}

fun asctime_r(arg0: tm?, arg1: String?): Int8Box? {
    val _arg0 = arg0.getNativePtr().asLong()
    val _arg1 = CString.fromString(arg1).getNativePtr().asLong()
    val res = externals.asctime_r(_arg0, _arg1)
    free(NativePtr.byValue(_arg1))
    return NativePtr.byValue(res).asRef(Int8Box)
}

fun ctime_r(arg0: Int64Box?, arg1: String?): Int8Box? {
    val _arg0 = arg0.getNativePtr().asLong()
    val _arg1 = CString.fromString(arg1).getNativePtr().asLong()
    val res = externals.ctime_r(_arg0, _arg1)
    free(NativePtr.byValue(_arg1))
    return NativePtr.byValue(res).asRef(Int8Box)
}

fun gmtime_r(arg0: Int64Box?, arg1: tm?): tm? {
    val _arg0 = arg0.getNativePtr().asLong()
    val _arg1 = arg1.getNativePtr().asLong()
    val res = externals.gmtime_r(_arg0, _arg1)
    return NativePtr.byValue(res).asRef(tm)
}

fun localtime_r(arg0: Int64Box?, arg1: tm?): tm? {
    val _arg0 = arg0.getNativePtr().asLong()
    val _arg1 = arg1.getNativePtr().asLong()
    val res = externals.localtime_r(_arg0, _arg1)
    return NativePtr.byValue(res).asRef(tm)
}

fun posix2time(arg0: Long): Long {
    val res = externals.posix2time(arg0)
    return res
}

fun tzsetwall(): Unit {
    val res = externals.tzsetwall()
    return res
}

fun time2posix(arg0: Long): Long {
    val res = externals.time2posix(arg0)
    return res
}

fun timelocal(arg0: tm?): Long {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.timelocal(_arg0)
    return res
}

fun timegm(arg0: tm?): Long {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.timegm(_arg0)
    return res
}

fun nanosleep(arg0: timespec?, arg1: timespec?): Int {
    val _arg0 = arg0.getNativePtr().asLong()
    val _arg1 = arg1.getNativePtr().asLong()
    val res = externals.nanosleep(_arg0, _arg1)
    return res
}

fun clang_getCString(string: CXString): Int8Box? {
    val _string = string.getNativePtr().asLong()
    val res = externals.clang_getCString(_string)
    return NativePtr.byValue(res).asRef(Int8Box)
}

fun clang_disposeString(string: CXString): Unit {
    val _string = string.getNativePtr().asLong()
    val res = externals.clang_disposeString(_string)
    return res
}

fun clang_disposeStringSet(set: CXStringSet?): Unit {
    val _set = set.getNativePtr().asLong()
    val res = externals.clang_disposeStringSet(_set)
    return res
}

fun clang_getBuildSessionTimestamp(): Long {
    val res = externals.clang_getBuildSessionTimestamp()
    return res
}

fun clang_VirtualFileOverlay_create(options: Int): CXVirtualFileOverlayImpl? {
    val res = externals.clang_VirtualFileOverlay_create(options)
    return NativePtr.byValue(res).asRef(CXVirtualFileOverlayImpl)
}

fun clang_VirtualFileOverlay_addFileMapping(arg0: CXVirtualFileOverlayImpl?, virtualPath: String?, realPath: String?): CXErrorCode {
    val _arg0 = arg0.getNativePtr().asLong()
    val _virtualPath = CString.fromString(virtualPath).getNativePtr().asLong()
    val _realPath = CString.fromString(realPath).getNativePtr().asLong()
    val res = externals.clang_VirtualFileOverlay_addFileMapping(_arg0, _virtualPath, _realPath)
    free(NativePtr.byValue(_virtualPath))
    free(NativePtr.byValue(_realPath))
    return CXErrorCode.byValue(res)
}

fun clang_VirtualFileOverlay_setCaseSensitivity(arg0: CXVirtualFileOverlayImpl?, caseSensitive: Int): CXErrorCode {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_VirtualFileOverlay_setCaseSensitivity(_arg0, caseSensitive)
    return CXErrorCode.byValue(res)
}

fun clang_VirtualFileOverlay_writeToBuffer(arg0: CXVirtualFileOverlayImpl?, options: Int, out_buffer_ptr: RefBox<Int8Box>?, out_buffer_size: Int32Box?): CXErrorCode {
    val _arg0 = arg0.getNativePtr().asLong()
    val _out_buffer_ptr = out_buffer_ptr.getNativePtr().asLong()
    val _out_buffer_size = out_buffer_size.getNativePtr().asLong()
    val res = externals.clang_VirtualFileOverlay_writeToBuffer(_arg0, options, _out_buffer_ptr, _out_buffer_size)
    return CXErrorCode.byValue(res)
}

fun clang_free(buffer: NativePtr?): Unit {
    val _buffer = buffer.asLong()
    val res = externals.clang_free(_buffer)
    return res
}

fun clang_VirtualFileOverlay_dispose(arg0: CXVirtualFileOverlayImpl?): Unit {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_VirtualFileOverlay_dispose(_arg0)
    return res
}

fun clang_ModuleMapDescriptor_create(options: Int): CXModuleMapDescriptorImpl? {
    val res = externals.clang_ModuleMapDescriptor_create(options)
    return NativePtr.byValue(res).asRef(CXModuleMapDescriptorImpl)
}

fun clang_ModuleMapDescriptor_setFrameworkModuleName(arg0: CXModuleMapDescriptorImpl?, name: String?): CXErrorCode {
    val _arg0 = arg0.getNativePtr().asLong()
    val _name = CString.fromString(name).getNativePtr().asLong()
    val res = externals.clang_ModuleMapDescriptor_setFrameworkModuleName(_arg0, _name)
    free(NativePtr.byValue(_name))
    return CXErrorCode.byValue(res)
}

fun clang_ModuleMapDescriptor_setUmbrellaHeader(arg0: CXModuleMapDescriptorImpl?, name: String?): CXErrorCode {
    val _arg0 = arg0.getNativePtr().asLong()
    val _name = CString.fromString(name).getNativePtr().asLong()
    val res = externals.clang_ModuleMapDescriptor_setUmbrellaHeader(_arg0, _name)
    free(NativePtr.byValue(_name))
    return CXErrorCode.byValue(res)
}

fun clang_ModuleMapDescriptor_writeToBuffer(arg0: CXModuleMapDescriptorImpl?, options: Int, out_buffer_ptr: RefBox<Int8Box>?, out_buffer_size: Int32Box?): CXErrorCode {
    val _arg0 = arg0.getNativePtr().asLong()
    val _out_buffer_ptr = out_buffer_ptr.getNativePtr().asLong()
    val _out_buffer_size = out_buffer_size.getNativePtr().asLong()
    val res = externals.clang_ModuleMapDescriptor_writeToBuffer(_arg0, options, _out_buffer_ptr, _out_buffer_size)
    return CXErrorCode.byValue(res)
}

fun clang_ModuleMapDescriptor_dispose(arg0: CXModuleMapDescriptorImpl?): Unit {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_ModuleMapDescriptor_dispose(_arg0)
    return res
}

fun clang_createIndex(excludeDeclarationsFromPCH: Int, displayDiagnostics: Int): NativePtr? {
    val res = externals.clang_createIndex(excludeDeclarationsFromPCH, displayDiagnostics)
    return NativePtr.byValue(res)
}

fun clang_disposeIndex(index: NativePtr?): Unit {
    val _index = index.asLong()
    val res = externals.clang_disposeIndex(_index)
    return res
}

fun clang_CXIndex_setGlobalOptions(arg0: NativePtr?, options: Int): Unit {
    val _arg0 = arg0.asLong()
    val res = externals.clang_CXIndex_setGlobalOptions(_arg0, options)
    return res
}

fun clang_CXIndex_getGlobalOptions(arg0: NativePtr?): Int {
    val _arg0 = arg0.asLong()
    val res = externals.clang_CXIndex_getGlobalOptions(_arg0)
    return res
}

fun clang_getFileName(SFile: NativePtr?, retValPlacement: Placement): CXString {
    val _SFile = SFile.asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getFileName(_SFile, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getFileTime(SFile: NativePtr?): Long {
    val _SFile = SFile.asLong()
    val res = externals.clang_getFileTime(_SFile)
    return res
}

fun clang_getFileUniqueID(file: NativePtr?, outID: CXFileUniqueID?): Int {
    val _file = file.asLong()
    val _outID = outID.getNativePtr().asLong()
    val res = externals.clang_getFileUniqueID(_file, _outID)
    return res
}

fun clang_isFileMultipleIncludeGuarded(tu: CXTranslationUnitImpl?, file: NativePtr?): Int {
    val _tu = tu.getNativePtr().asLong()
    val _file = file.asLong()
    val res = externals.clang_isFileMultipleIncludeGuarded(_tu, _file)
    return res
}

fun clang_getFile(tu: CXTranslationUnitImpl?, file_name: String?): NativePtr? {
    val _tu = tu.getNativePtr().asLong()
    val _file_name = CString.fromString(file_name).getNativePtr().asLong()
    val res = externals.clang_getFile(_tu, _file_name)
    free(NativePtr.byValue(_file_name))
    return NativePtr.byValue(res)
}

fun clang_File_isEqual(file1: NativePtr?, file2: NativePtr?): Int {
    val _file1 = file1.asLong()
    val _file2 = file2.asLong()
    val res = externals.clang_File_isEqual(_file1, _file2)
    return res
}

fun clang_getNullLocation(retValPlacement: Placement): CXSourceLocation {
    val _retValPlacement = retValPlacement.alloc(CXSourceLocation.size).asLong()
    val res = externals.clang_getNullLocation(_retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceLocation)!!
}

fun clang_equalLocations(loc1: CXSourceLocation, loc2: CXSourceLocation): Int {
    val _loc1 = loc1.getNativePtr().asLong()
    val _loc2 = loc2.getNativePtr().asLong()
    val res = externals.clang_equalLocations(_loc1, _loc2)
    return res
}

fun clang_getLocation(tu: CXTranslationUnitImpl?, file: NativePtr?, line: Int, column: Int, retValPlacement: Placement): CXSourceLocation {
    val _tu = tu.getNativePtr().asLong()
    val _file = file.asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceLocation.size).asLong()
    val res = externals.clang_getLocation(_tu, _file, line, column, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceLocation)!!
}

fun clang_getLocationForOffset(tu: CXTranslationUnitImpl?, file: NativePtr?, offset: Int, retValPlacement: Placement): CXSourceLocation {
    val _tu = tu.getNativePtr().asLong()
    val _file = file.asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceLocation.size).asLong()
    val res = externals.clang_getLocationForOffset(_tu, _file, offset, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceLocation)!!
}

fun clang_Location_isInSystemHeader(location: CXSourceLocation): Int {
    val _location = location.getNativePtr().asLong()
    val res = externals.clang_Location_isInSystemHeader(_location)
    return res
}

fun clang_Location_isFromMainFile(location: CXSourceLocation): Int {
    val _location = location.getNativePtr().asLong()
    val res = externals.clang_Location_isFromMainFile(_location)
    return res
}

fun clang_getNullRange(retValPlacement: Placement): CXSourceRange {
    val _retValPlacement = retValPlacement.alloc(CXSourceRange.size).asLong()
    val res = externals.clang_getNullRange(_retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceRange)!!
}

fun clang_getRange(begin: CXSourceLocation, end: CXSourceLocation, retValPlacement: Placement): CXSourceRange {
    val _begin = begin.getNativePtr().asLong()
    val _end = end.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceRange.size).asLong()
    val res = externals.clang_getRange(_begin, _end, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceRange)!!
}

fun clang_equalRanges(range1: CXSourceRange, range2: CXSourceRange): Int {
    val _range1 = range1.getNativePtr().asLong()
    val _range2 = range2.getNativePtr().asLong()
    val res = externals.clang_equalRanges(_range1, _range2)
    return res
}

fun clang_Range_isNull(range: CXSourceRange): Int {
    val _range = range.getNativePtr().asLong()
    val res = externals.clang_Range_isNull(_range)
    return res
}

fun clang_getExpansionLocation(location: CXSourceLocation, file: NativePtrBox?, line: Int32Box?, column: Int32Box?, offset: Int32Box?): Unit {
    val _location = location.getNativePtr().asLong()
    val _file = file.getNativePtr().asLong()
    val _line = line.getNativePtr().asLong()
    val _column = column.getNativePtr().asLong()
    val _offset = offset.getNativePtr().asLong()
    val res = externals.clang_getExpansionLocation(_location, _file, _line, _column, _offset)
    return res
}

fun clang_getPresumedLocation(location: CXSourceLocation, filename: CXString?, line: Int32Box?, column: Int32Box?): Unit {
    val _location = location.getNativePtr().asLong()
    val _filename = filename.getNativePtr().asLong()
    val _line = line.getNativePtr().asLong()
    val _column = column.getNativePtr().asLong()
    val res = externals.clang_getPresumedLocation(_location, _filename, _line, _column)
    return res
}

fun clang_getInstantiationLocation(location: CXSourceLocation, file: NativePtrBox?, line: Int32Box?, column: Int32Box?, offset: Int32Box?): Unit {
    val _location = location.getNativePtr().asLong()
    val _file = file.getNativePtr().asLong()
    val _line = line.getNativePtr().asLong()
    val _column = column.getNativePtr().asLong()
    val _offset = offset.getNativePtr().asLong()
    val res = externals.clang_getInstantiationLocation(_location, _file, _line, _column, _offset)
    return res
}

fun clang_getSpellingLocation(location: CXSourceLocation, file: NativePtrBox?, line: Int32Box?, column: Int32Box?, offset: Int32Box?): Unit {
    val _location = location.getNativePtr().asLong()
    val _file = file.getNativePtr().asLong()
    val _line = line.getNativePtr().asLong()
    val _column = column.getNativePtr().asLong()
    val _offset = offset.getNativePtr().asLong()
    val res = externals.clang_getSpellingLocation(_location, _file, _line, _column, _offset)
    return res
}

fun clang_getFileLocation(location: CXSourceLocation, file: NativePtrBox?, line: Int32Box?, column: Int32Box?, offset: Int32Box?): Unit {
    val _location = location.getNativePtr().asLong()
    val _file = file.getNativePtr().asLong()
    val _line = line.getNativePtr().asLong()
    val _column = column.getNativePtr().asLong()
    val _offset = offset.getNativePtr().asLong()
    val res = externals.clang_getFileLocation(_location, _file, _line, _column, _offset)
    return res
}

fun clang_getRangeStart(range: CXSourceRange, retValPlacement: Placement): CXSourceLocation {
    val _range = range.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceLocation.size).asLong()
    val res = externals.clang_getRangeStart(_range, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceLocation)!!
}

fun clang_getRangeEnd(range: CXSourceRange, retValPlacement: Placement): CXSourceLocation {
    val _range = range.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceLocation.size).asLong()
    val res = externals.clang_getRangeEnd(_range, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceLocation)!!
}

fun clang_getSkippedRanges(tu: CXTranslationUnitImpl?, file: NativePtr?): CXSourceRangeList? {
    val _tu = tu.getNativePtr().asLong()
    val _file = file.asLong()
    val res = externals.clang_getSkippedRanges(_tu, _file)
    return NativePtr.byValue(res).asRef(CXSourceRangeList)
}

fun clang_disposeSourceRangeList(ranges: CXSourceRangeList?): Unit {
    val _ranges = ranges.getNativePtr().asLong()
    val res = externals.clang_disposeSourceRangeList(_ranges)
    return res
}

fun clang_getNumDiagnosticsInSet(Diags: NativePtr?): Int {
    val _Diags = Diags.asLong()
    val res = externals.clang_getNumDiagnosticsInSet(_Diags)
    return res
}

fun clang_getDiagnosticInSet(Diags: NativePtr?, Index: Int): NativePtr? {
    val _Diags = Diags.asLong()
    val res = externals.clang_getDiagnosticInSet(_Diags, Index)
    return NativePtr.byValue(res)
}

fun clang_loadDiagnostics(file: String?, error: CXLoadDiag_Error.ref?, errorString: CXString?): NativePtr? {
    val _file = CString.fromString(file).getNativePtr().asLong()
    val _error = error.getNativePtr().asLong()
    val _errorString = errorString.getNativePtr().asLong()
    val res = externals.clang_loadDiagnostics(_file, _error, _errorString)
    free(NativePtr.byValue(_file))
    return NativePtr.byValue(res)
}

fun clang_disposeDiagnosticSet(Diags: NativePtr?): Unit {
    val _Diags = Diags.asLong()
    val res = externals.clang_disposeDiagnosticSet(_Diags)
    return res
}

fun clang_getChildDiagnostics(D: NativePtr?): NativePtr? {
    val _D = D.asLong()
    val res = externals.clang_getChildDiagnostics(_D)
    return NativePtr.byValue(res)
}

fun clang_getNumDiagnostics(Unit: CXTranslationUnitImpl?): Int {
    val _Unit = Unit.getNativePtr().asLong()
    val res = externals.clang_getNumDiagnostics(_Unit)
    return res
}

fun clang_getDiagnostic(Unit: CXTranslationUnitImpl?, Index: Int): NativePtr? {
    val _Unit = Unit.getNativePtr().asLong()
    val res = externals.clang_getDiagnostic(_Unit, Index)
    return NativePtr.byValue(res)
}

fun clang_getDiagnosticSetFromTU(Unit: CXTranslationUnitImpl?): NativePtr? {
    val _Unit = Unit.getNativePtr().asLong()
    val res = externals.clang_getDiagnosticSetFromTU(_Unit)
    return NativePtr.byValue(res)
}

fun clang_disposeDiagnostic(Diagnostic: NativePtr?): Unit {
    val _Diagnostic = Diagnostic.asLong()
    val res = externals.clang_disposeDiagnostic(_Diagnostic)
    return res
}

fun clang_formatDiagnostic(Diagnostic: NativePtr?, Options: Int, retValPlacement: Placement): CXString {
    val _Diagnostic = Diagnostic.asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_formatDiagnostic(_Diagnostic, Options, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_defaultDiagnosticDisplayOptions(): Int {
    val res = externals.clang_defaultDiagnosticDisplayOptions()
    return res
}

fun clang_getDiagnosticSeverity(arg0: NativePtr?): CXDiagnosticSeverity {
    val _arg0 = arg0.asLong()
    val res = externals.clang_getDiagnosticSeverity(_arg0)
    return CXDiagnosticSeverity.byValue(res)
}

fun clang_getDiagnosticLocation(arg0: NativePtr?, retValPlacement: Placement): CXSourceLocation {
    val _arg0 = arg0.asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceLocation.size).asLong()
    val res = externals.clang_getDiagnosticLocation(_arg0, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceLocation)!!
}

fun clang_getDiagnosticSpelling(arg0: NativePtr?, retValPlacement: Placement): CXString {
    val _arg0 = arg0.asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getDiagnosticSpelling(_arg0, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getDiagnosticOption(Diag: NativePtr?, Disable: CXString?, retValPlacement: Placement): CXString {
    val _Diag = Diag.asLong()
    val _Disable = Disable.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getDiagnosticOption(_Diag, _Disable, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getDiagnosticCategory(arg0: NativePtr?): Int {
    val _arg0 = arg0.asLong()
    val res = externals.clang_getDiagnosticCategory(_arg0)
    return res
}

fun clang_getDiagnosticCategoryName(Category: Int, retValPlacement: Placement): CXString {
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getDiagnosticCategoryName(Category, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getDiagnosticCategoryText(arg0: NativePtr?, retValPlacement: Placement): CXString {
    val _arg0 = arg0.asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getDiagnosticCategoryText(_arg0, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getDiagnosticNumRanges(arg0: NativePtr?): Int {
    val _arg0 = arg0.asLong()
    val res = externals.clang_getDiagnosticNumRanges(_arg0)
    return res
}

fun clang_getDiagnosticRange(Diagnostic: NativePtr?, Range: Int, retValPlacement: Placement): CXSourceRange {
    val _Diagnostic = Diagnostic.asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceRange.size).asLong()
    val res = externals.clang_getDiagnosticRange(_Diagnostic, Range, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceRange)!!
}

fun clang_getDiagnosticNumFixIts(Diagnostic: NativePtr?): Int {
    val _Diagnostic = Diagnostic.asLong()
    val res = externals.clang_getDiagnosticNumFixIts(_Diagnostic)
    return res
}

fun clang_getDiagnosticFixIt(Diagnostic: NativePtr?, FixIt: Int, ReplacementRange: CXSourceRange?, retValPlacement: Placement): CXString {
    val _Diagnostic = Diagnostic.asLong()
    val _ReplacementRange = ReplacementRange.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getDiagnosticFixIt(_Diagnostic, FixIt, _ReplacementRange, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getTranslationUnitSpelling(CTUnit: CXTranslationUnitImpl?, retValPlacement: Placement): CXString {
    val _CTUnit = CTUnit.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getTranslationUnitSpelling(_CTUnit, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_createTranslationUnitFromSourceFile(CIdx: NativePtr?, source_filename: String?, num_clang_command_line_args: Int, clang_command_line_args: RefBox<Int8Box>?, num_unsaved_files: Int, unsaved_files: CXUnsavedFile?): CXTranslationUnitImpl? {
    val _CIdx = CIdx.asLong()
    val _source_filename = CString.fromString(source_filename).getNativePtr().asLong()
    val _clang_command_line_args = clang_command_line_args.getNativePtr().asLong()
    val _unsaved_files = unsaved_files.getNativePtr().asLong()
    val res = externals.clang_createTranslationUnitFromSourceFile(_CIdx, _source_filename, num_clang_command_line_args, _clang_command_line_args, num_unsaved_files, _unsaved_files)
    free(NativePtr.byValue(_source_filename))
    return NativePtr.byValue(res).asRef(CXTranslationUnitImpl)
}

fun clang_createTranslationUnit(CIdx: NativePtr?, ast_filename: String?): CXTranslationUnitImpl? {
    val _CIdx = CIdx.asLong()
    val _ast_filename = CString.fromString(ast_filename).getNativePtr().asLong()
    val res = externals.clang_createTranslationUnit(_CIdx, _ast_filename)
    free(NativePtr.byValue(_ast_filename))
    return NativePtr.byValue(res).asRef(CXTranslationUnitImpl)
}

fun clang_createTranslationUnit2(CIdx: NativePtr?, ast_filename: String?, out_TU: RefBox<CXTranslationUnitImpl>?): CXErrorCode {
    val _CIdx = CIdx.asLong()
    val _ast_filename = CString.fromString(ast_filename).getNativePtr().asLong()
    val _out_TU = out_TU.getNativePtr().asLong()
    val res = externals.clang_createTranslationUnit2(_CIdx, _ast_filename, _out_TU)
    free(NativePtr.byValue(_ast_filename))
    return CXErrorCode.byValue(res)
}

fun clang_defaultEditingTranslationUnitOptions(): Int {
    val res = externals.clang_defaultEditingTranslationUnitOptions()
    return res
}

fun clang_parseTranslationUnit(CIdx: NativePtr?, source_filename: String?, command_line_args: RefBox<Int8Box>?, num_command_line_args: Int, unsaved_files: CXUnsavedFile?, num_unsaved_files: Int, options: Int): CXTranslationUnitImpl? {
    val _CIdx = CIdx.asLong()
    val _source_filename = CString.fromString(source_filename).getNativePtr().asLong()
    val _command_line_args = command_line_args.getNativePtr().asLong()
    val _unsaved_files = unsaved_files.getNativePtr().asLong()
    val res = externals.clang_parseTranslationUnit(_CIdx, _source_filename, _command_line_args, num_command_line_args, _unsaved_files, num_unsaved_files, options)
    free(NativePtr.byValue(_source_filename))
    return NativePtr.byValue(res).asRef(CXTranslationUnitImpl)
}

fun clang_parseTranslationUnit2(CIdx: NativePtr?, source_filename: String?, command_line_args: RefBox<Int8Box>?, num_command_line_args: Int, unsaved_files: CXUnsavedFile?, num_unsaved_files: Int, options: Int, out_TU: RefBox<CXTranslationUnitImpl>?): CXErrorCode {
    val _CIdx = CIdx.asLong()
    val _source_filename = CString.fromString(source_filename).getNativePtr().asLong()
    val _command_line_args = command_line_args.getNativePtr().asLong()
    val _unsaved_files = unsaved_files.getNativePtr().asLong()
    val _out_TU = out_TU.getNativePtr().asLong()
    val res = externals.clang_parseTranslationUnit2(_CIdx, _source_filename, _command_line_args, num_command_line_args, _unsaved_files, num_unsaved_files, options, _out_TU)
    free(NativePtr.byValue(_source_filename))
    return CXErrorCode.byValue(res)
}

fun clang_parseTranslationUnit2FullArgv(CIdx: NativePtr?, source_filename: String?, command_line_args: RefBox<Int8Box>?, num_command_line_args: Int, unsaved_files: CXUnsavedFile?, num_unsaved_files: Int, options: Int, out_TU: RefBox<CXTranslationUnitImpl>?): CXErrorCode {
    val _CIdx = CIdx.asLong()
    val _source_filename = CString.fromString(source_filename).getNativePtr().asLong()
    val _command_line_args = command_line_args.getNativePtr().asLong()
    val _unsaved_files = unsaved_files.getNativePtr().asLong()
    val _out_TU = out_TU.getNativePtr().asLong()
    val res = externals.clang_parseTranslationUnit2FullArgv(_CIdx, _source_filename, _command_line_args, num_command_line_args, _unsaved_files, num_unsaved_files, options, _out_TU)
    free(NativePtr.byValue(_source_filename))
    return CXErrorCode.byValue(res)
}

fun clang_defaultSaveOptions(TU: CXTranslationUnitImpl?): Int {
    val _TU = TU.getNativePtr().asLong()
    val res = externals.clang_defaultSaveOptions(_TU)
    return res
}

fun clang_saveTranslationUnit(TU: CXTranslationUnitImpl?, FileName: String?, options: Int): Int {
    val _TU = TU.getNativePtr().asLong()
    val _FileName = CString.fromString(FileName).getNativePtr().asLong()
    val res = externals.clang_saveTranslationUnit(_TU, _FileName, options)
    free(NativePtr.byValue(_FileName))
    return res
}

fun clang_disposeTranslationUnit(arg0: CXTranslationUnitImpl?): Unit {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_disposeTranslationUnit(_arg0)
    return res
}

fun clang_defaultReparseOptions(TU: CXTranslationUnitImpl?): Int {
    val _TU = TU.getNativePtr().asLong()
    val res = externals.clang_defaultReparseOptions(_TU)
    return res
}

fun clang_reparseTranslationUnit(TU: CXTranslationUnitImpl?, num_unsaved_files: Int, unsaved_files: CXUnsavedFile?, options: Int): Int {
    val _TU = TU.getNativePtr().asLong()
    val _unsaved_files = unsaved_files.getNativePtr().asLong()
    val res = externals.clang_reparseTranslationUnit(_TU, num_unsaved_files, _unsaved_files, options)
    return res
}

fun clang_getTUResourceUsageName(kind: CXTUResourceUsageKind): Int8Box? {
    val _kind = kind.value
    val res = externals.clang_getTUResourceUsageName(_kind)
    return NativePtr.byValue(res).asRef(Int8Box)
}

fun clang_getCXTUResourceUsage(TU: CXTranslationUnitImpl?, retValPlacement: Placement): CXTUResourceUsage {
    val _TU = TU.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXTUResourceUsage.size).asLong()
    val res = externals.clang_getCXTUResourceUsage(_TU, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXTUResourceUsage)!!
}

fun clang_disposeCXTUResourceUsage(usage: CXTUResourceUsage): Unit {
    val _usage = usage.getNativePtr().asLong()
    val res = externals.clang_disposeCXTUResourceUsage(_usage)
    return res
}

fun clang_getNullCursor(retValPlacement: Placement): CXCursor {
    val _retValPlacement = retValPlacement.alloc(CXCursor.size).asLong()
    val res = externals.clang_getNullCursor(_retValPlacement)
    return NativePtr.byValue(res).asRef(CXCursor)!!
}

fun clang_getTranslationUnitCursor(arg0: CXTranslationUnitImpl?, retValPlacement: Placement): CXCursor {
    val _arg0 = arg0.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXCursor.size).asLong()
    val res = externals.clang_getTranslationUnitCursor(_arg0, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXCursor)!!
}

fun clang_equalCursors(arg0: CXCursor, arg1: CXCursor): Int {
    val _arg0 = arg0.getNativePtr().asLong()
    val _arg1 = arg1.getNativePtr().asLong()
    val res = externals.clang_equalCursors(_arg0, _arg1)
    return res
}

fun clang_Cursor_isNull(cursor: CXCursor): Int {
    val _cursor = cursor.getNativePtr().asLong()
    val res = externals.clang_Cursor_isNull(_cursor)
    return res
}

fun clang_hashCursor(arg0: CXCursor): Int {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_hashCursor(_arg0)
    return res
}

fun clang_getCursorKind(arg0: CXCursor): CXCursorKind {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_getCursorKind(_arg0)
    return CXCursorKind.byValue(res)
}

fun clang_isDeclaration(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = externals.clang_isDeclaration(_arg0)
    return res
}

fun clang_isReference(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = externals.clang_isReference(_arg0)
    return res
}

fun clang_isExpression(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = externals.clang_isExpression(_arg0)
    return res
}

fun clang_isStatement(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = externals.clang_isStatement(_arg0)
    return res
}

fun clang_isAttribute(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = externals.clang_isAttribute(_arg0)
    return res
}

fun clang_isInvalid(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = externals.clang_isInvalid(_arg0)
    return res
}

fun clang_isTranslationUnit(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = externals.clang_isTranslationUnit(_arg0)
    return res
}

fun clang_isPreprocessing(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = externals.clang_isPreprocessing(_arg0)
    return res
}

fun clang_isUnexposed(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = externals.clang_isUnexposed(_arg0)
    return res
}

fun clang_getCursorLinkage(cursor: CXCursor): CXLinkageKind {
    val _cursor = cursor.getNativePtr().asLong()
    val res = externals.clang_getCursorLinkage(_cursor)
    return CXLinkageKind.byValue(res)
}

fun clang_getCursorVisibility(cursor: CXCursor): CXVisibilityKind {
    val _cursor = cursor.getNativePtr().asLong()
    val res = externals.clang_getCursorVisibility(_cursor)
    return CXVisibilityKind.byValue(res)
}

fun clang_getCursorAvailability(cursor: CXCursor): CXAvailabilityKind {
    val _cursor = cursor.getNativePtr().asLong()
    val res = externals.clang_getCursorAvailability(_cursor)
    return CXAvailabilityKind.byValue(res)
}

fun clang_getCursorPlatformAvailability(cursor: CXCursor, always_deprecated: Int32Box?, deprecated_message: CXString?, always_unavailable: Int32Box?, unavailable_message: CXString?, availability: CXPlatformAvailability?, availability_size: Int): Int {
    val _cursor = cursor.getNativePtr().asLong()
    val _always_deprecated = always_deprecated.getNativePtr().asLong()
    val _deprecated_message = deprecated_message.getNativePtr().asLong()
    val _always_unavailable = always_unavailable.getNativePtr().asLong()
    val _unavailable_message = unavailable_message.getNativePtr().asLong()
    val _availability = availability.getNativePtr().asLong()
    val res = externals.clang_getCursorPlatformAvailability(_cursor, _always_deprecated, _deprecated_message, _always_unavailable, _unavailable_message, _availability, availability_size)
    return res
}

fun clang_disposeCXPlatformAvailability(availability: CXPlatformAvailability?): Unit {
    val _availability = availability.getNativePtr().asLong()
    val res = externals.clang_disposeCXPlatformAvailability(_availability)
    return res
}

fun clang_getCursorLanguage(cursor: CXCursor): CXLanguageKind {
    val _cursor = cursor.getNativePtr().asLong()
    val res = externals.clang_getCursorLanguage(_cursor)
    return CXLanguageKind.byValue(res)
}

fun clang_Cursor_getTranslationUnit(arg0: CXCursor): CXTranslationUnitImpl? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_Cursor_getTranslationUnit(_arg0)
    return NativePtr.byValue(res).asRef(CXTranslationUnitImpl)
}

fun clang_createCXCursorSet(): CXCursorSetImpl? {
    val res = externals.clang_createCXCursorSet()
    return NativePtr.byValue(res).asRef(CXCursorSetImpl)
}

fun clang_disposeCXCursorSet(cset: CXCursorSetImpl?): Unit {
    val _cset = cset.getNativePtr().asLong()
    val res = externals.clang_disposeCXCursorSet(_cset)
    return res
}

fun clang_CXCursorSet_contains(cset: CXCursorSetImpl?, cursor: CXCursor): Int {
    val _cset = cset.getNativePtr().asLong()
    val _cursor = cursor.getNativePtr().asLong()
    val res = externals.clang_CXCursorSet_contains(_cset, _cursor)
    return res
}

fun clang_CXCursorSet_insert(cset: CXCursorSetImpl?, cursor: CXCursor): Int {
    val _cset = cset.getNativePtr().asLong()
    val _cursor = cursor.getNativePtr().asLong()
    val res = externals.clang_CXCursorSet_insert(_cset, _cursor)
    return res
}

fun clang_getCursorSemanticParent(cursor: CXCursor, retValPlacement: Placement): CXCursor {
    val _cursor = cursor.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXCursor.size).asLong()
    val res = externals.clang_getCursorSemanticParent(_cursor, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXCursor)!!
}

fun clang_getCursorLexicalParent(cursor: CXCursor, retValPlacement: Placement): CXCursor {
    val _cursor = cursor.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXCursor.size).asLong()
    val res = externals.clang_getCursorLexicalParent(_cursor, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXCursor)!!
}

fun clang_getOverriddenCursors(cursor: CXCursor, overridden: RefBox<CXCursor>?, num_overridden: Int32Box?): Unit {
    val _cursor = cursor.getNativePtr().asLong()
    val _overridden = overridden.getNativePtr().asLong()
    val _num_overridden = num_overridden.getNativePtr().asLong()
    val res = externals.clang_getOverriddenCursors(_cursor, _overridden, _num_overridden)
    return res
}

fun clang_disposeOverriddenCursors(overridden: CXCursor?): Unit {
    val _overridden = overridden.getNativePtr().asLong()
    val res = externals.clang_disposeOverriddenCursors(_overridden)
    return res
}

fun clang_getIncludedFile(cursor: CXCursor): NativePtr? {
    val _cursor = cursor.getNativePtr().asLong()
    val res = externals.clang_getIncludedFile(_cursor)
    return NativePtr.byValue(res)
}

fun clang_getCursor(arg0: CXTranslationUnitImpl?, arg1: CXSourceLocation, retValPlacement: Placement): CXCursor {
    val _arg0 = arg0.getNativePtr().asLong()
    val _arg1 = arg1.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXCursor.size).asLong()
    val res = externals.clang_getCursor(_arg0, _arg1, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXCursor)!!
}

fun clang_getCursorLocation(arg0: CXCursor, retValPlacement: Placement): CXSourceLocation {
    val _arg0 = arg0.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceLocation.size).asLong()
    val res = externals.clang_getCursorLocation(_arg0, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceLocation)!!
}

fun clang_getCursorExtent(arg0: CXCursor, retValPlacement: Placement): CXSourceRange {
    val _arg0 = arg0.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceRange.size).asLong()
    val res = externals.clang_getCursorExtent(_arg0, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceRange)!!
}

fun clang_getCursorType(C: CXCursor, retValPlacement: Placement): CXType {
    val _C = C.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_getCursorType(_C, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_getTypeSpelling(CT: CXType, retValPlacement: Placement): CXString {
    val _CT = CT.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getTypeSpelling(_CT, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getTypedefDeclUnderlyingType(C: CXCursor, retValPlacement: Placement): CXType {
    val _C = C.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_getTypedefDeclUnderlyingType(_C, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_getEnumDeclIntegerType(C: CXCursor, retValPlacement: Placement): CXType {
    val _C = C.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_getEnumDeclIntegerType(_C, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_getEnumConstantDeclValue(C: CXCursor): Long {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_getEnumConstantDeclValue(_C)
    return res
}

fun clang_getEnumConstantDeclUnsignedValue(C: CXCursor): Long {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_getEnumConstantDeclUnsignedValue(_C)
    return res
}

fun clang_getFieldDeclBitWidth(C: CXCursor): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_getFieldDeclBitWidth(_C)
    return res
}

fun clang_Cursor_getNumArguments(C: CXCursor): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_Cursor_getNumArguments(_C)
    return res
}

fun clang_Cursor_getArgument(C: CXCursor, i: Int, retValPlacement: Placement): CXCursor {
    val _C = C.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXCursor.size).asLong()
    val res = externals.clang_Cursor_getArgument(_C, i, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXCursor)!!
}

fun clang_Cursor_getNumTemplateArguments(C: CXCursor): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_Cursor_getNumTemplateArguments(_C)
    return res
}

fun clang_Cursor_getTemplateArgumentKind(C: CXCursor, I: Int): CXTemplateArgumentKind {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_Cursor_getTemplateArgumentKind(_C, I)
    return CXTemplateArgumentKind.byValue(res)
}

fun clang_Cursor_getTemplateArgumentType(C: CXCursor, I: Int, retValPlacement: Placement): CXType {
    val _C = C.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_Cursor_getTemplateArgumentType(_C, I, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_Cursor_getTemplateArgumentValue(C: CXCursor, I: Int): Long {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_Cursor_getTemplateArgumentValue(_C, I)
    return res
}

fun clang_Cursor_getTemplateArgumentUnsignedValue(C: CXCursor, I: Int): Long {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_Cursor_getTemplateArgumentUnsignedValue(_C, I)
    return res
}

fun clang_equalTypes(A: CXType, B: CXType): Int {
    val _A = A.getNativePtr().asLong()
    val _B = B.getNativePtr().asLong()
    val res = externals.clang_equalTypes(_A, _B)
    return res
}

fun clang_getCanonicalType(T: CXType, retValPlacement: Placement): CXType {
    val _T = T.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_getCanonicalType(_T, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_isConstQualifiedType(T: CXType): Int {
    val _T = T.getNativePtr().asLong()
    val res = externals.clang_isConstQualifiedType(_T)
    return res
}

fun clang_isVolatileQualifiedType(T: CXType): Int {
    val _T = T.getNativePtr().asLong()
    val res = externals.clang_isVolatileQualifiedType(_T)
    return res
}

fun clang_isRestrictQualifiedType(T: CXType): Int {
    val _T = T.getNativePtr().asLong()
    val res = externals.clang_isRestrictQualifiedType(_T)
    return res
}

fun clang_getPointeeType(T: CXType, retValPlacement: Placement): CXType {
    val _T = T.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_getPointeeType(_T, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_getTypeDeclaration(T: CXType, retValPlacement: Placement): CXCursor {
    val _T = T.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXCursor.size).asLong()
    val res = externals.clang_getTypeDeclaration(_T, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXCursor)!!
}

fun clang_getDeclObjCTypeEncoding(C: CXCursor, retValPlacement: Placement): CXString {
    val _C = C.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getDeclObjCTypeEncoding(_C, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getTypeKindSpelling(K: CXTypeKind, retValPlacement: Placement): CXString {
    val _K = K.value
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getTypeKindSpelling(_K, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getFunctionTypeCallingConv(T: CXType): CXCallingConv {
    val _T = T.getNativePtr().asLong()
    val res = externals.clang_getFunctionTypeCallingConv(_T)
    return CXCallingConv.byValue(res)
}

fun clang_getResultType(T: CXType, retValPlacement: Placement): CXType {
    val _T = T.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_getResultType(_T, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_getNumArgTypes(T: CXType): Int {
    val _T = T.getNativePtr().asLong()
    val res = externals.clang_getNumArgTypes(_T)
    return res
}

fun clang_getArgType(T: CXType, i: Int, retValPlacement: Placement): CXType {
    val _T = T.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_getArgType(_T, i, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_isFunctionTypeVariadic(T: CXType): Int {
    val _T = T.getNativePtr().asLong()
    val res = externals.clang_isFunctionTypeVariadic(_T)
    return res
}

fun clang_getCursorResultType(C: CXCursor, retValPlacement: Placement): CXType {
    val _C = C.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_getCursorResultType(_C, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_isPODType(T: CXType): Int {
    val _T = T.getNativePtr().asLong()
    val res = externals.clang_isPODType(_T)
    return res
}

fun clang_getElementType(T: CXType, retValPlacement: Placement): CXType {
    val _T = T.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_getElementType(_T, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_getNumElements(T: CXType): Long {
    val _T = T.getNativePtr().asLong()
    val res = externals.clang_getNumElements(_T)
    return res
}

fun clang_getArrayElementType(T: CXType, retValPlacement: Placement): CXType {
    val _T = T.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_getArrayElementType(_T, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_getArraySize(T: CXType): Long {
    val _T = T.getNativePtr().asLong()
    val res = externals.clang_getArraySize(_T)
    return res
}

fun clang_Type_getAlignOf(T: CXType): Long {
    val _T = T.getNativePtr().asLong()
    val res = externals.clang_Type_getAlignOf(_T)
    return res
}

fun clang_Type_getClassType(T: CXType, retValPlacement: Placement): CXType {
    val _T = T.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_Type_getClassType(_T, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_Type_getSizeOf(T: CXType): Long {
    val _T = T.getNativePtr().asLong()
    val res = externals.clang_Type_getSizeOf(_T)
    return res
}

fun clang_Type_getOffsetOf(T: CXType, S: String?): Long {
    val _T = T.getNativePtr().asLong()
    val _S = CString.fromString(S).getNativePtr().asLong()
    val res = externals.clang_Type_getOffsetOf(_T, _S)
    free(NativePtr.byValue(_S))
    return res
}

fun clang_Cursor_getOffsetOfField(C: CXCursor): Long {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_Cursor_getOffsetOfField(_C)
    return res
}

fun clang_Cursor_isAnonymous(C: CXCursor): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_Cursor_isAnonymous(_C)
    return res
}

fun clang_Type_getNumTemplateArguments(T: CXType): Int {
    val _T = T.getNativePtr().asLong()
    val res = externals.clang_Type_getNumTemplateArguments(_T)
    return res
}

fun clang_Type_getTemplateArgumentAsType(T: CXType, i: Int, retValPlacement: Placement): CXType {
    val _T = T.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_Type_getTemplateArgumentAsType(_T, i, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_Type_getCXXRefQualifier(T: CXType): CXRefQualifierKind {
    val _T = T.getNativePtr().asLong()
    val res = externals.clang_Type_getCXXRefQualifier(_T)
    return CXRefQualifierKind.byValue(res)
}

fun clang_Cursor_isBitField(C: CXCursor): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_Cursor_isBitField(_C)
    return res
}

fun clang_isVirtualBase(arg0: CXCursor): Int {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_isVirtualBase(_arg0)
    return res
}

fun clang_getCXXAccessSpecifier(arg0: CXCursor): CX_CXXAccessSpecifier {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_getCXXAccessSpecifier(_arg0)
    return CX_CXXAccessSpecifier.byValue(res)
}

fun clang_Cursor_getStorageClass(arg0: CXCursor): CX_StorageClass {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_Cursor_getStorageClass(_arg0)
    return CX_StorageClass.byValue(res)
}

fun clang_getNumOverloadedDecls(cursor: CXCursor): Int {
    val _cursor = cursor.getNativePtr().asLong()
    val res = externals.clang_getNumOverloadedDecls(_cursor)
    return res
}

fun clang_getOverloadedDecl(cursor: CXCursor, index: Int, retValPlacement: Placement): CXCursor {
    val _cursor = cursor.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXCursor.size).asLong()
    val res = externals.clang_getOverloadedDecl(_cursor, index, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXCursor)!!
}

fun clang_getIBOutletCollectionType(arg0: CXCursor, retValPlacement: Placement): CXType {
    val _arg0 = arg0.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_getIBOutletCollectionType(_arg0, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_visitChildren(parent: CXCursor, visitor: NativePtr?, client_data: NativePtr?): Int {
    val _parent = parent.getNativePtr().asLong()
    val _visitor = visitor.asLong()
    val _client_data = client_data.asLong()
    val res = externals.clang_visitChildren(_parent, _visitor, _client_data)
    return res
}

fun clang_getCursorUSR(arg0: CXCursor, retValPlacement: Placement): CXString {
    val _arg0 = arg0.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getCursorUSR(_arg0, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_constructUSR_ObjCClass(class_name: String?, retValPlacement: Placement): CXString {
    val _class_name = CString.fromString(class_name).getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_constructUSR_ObjCClass(_class_name, _retValPlacement)
    free(NativePtr.byValue(_class_name))
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_constructUSR_ObjCCategory(class_name: String?, category_name: String?, retValPlacement: Placement): CXString {
    val _class_name = CString.fromString(class_name).getNativePtr().asLong()
    val _category_name = CString.fromString(category_name).getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_constructUSR_ObjCCategory(_class_name, _category_name, _retValPlacement)
    free(NativePtr.byValue(_class_name))
    free(NativePtr.byValue(_category_name))
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_constructUSR_ObjCProtocol(protocol_name: String?, retValPlacement: Placement): CXString {
    val _protocol_name = CString.fromString(protocol_name).getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_constructUSR_ObjCProtocol(_protocol_name, _retValPlacement)
    free(NativePtr.byValue(_protocol_name))
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_constructUSR_ObjCIvar(name: String?, classUSR: CXString, retValPlacement: Placement): CXString {
    val _name = CString.fromString(name).getNativePtr().asLong()
    val _classUSR = classUSR.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_constructUSR_ObjCIvar(_name, _classUSR, _retValPlacement)
    free(NativePtr.byValue(_name))
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_constructUSR_ObjCMethod(name: String?, isInstanceMethod: Int, classUSR: CXString, retValPlacement: Placement): CXString {
    val _name = CString.fromString(name).getNativePtr().asLong()
    val _classUSR = classUSR.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_constructUSR_ObjCMethod(_name, isInstanceMethod, _classUSR, _retValPlacement)
    free(NativePtr.byValue(_name))
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_constructUSR_ObjCProperty(property: String?, classUSR: CXString, retValPlacement: Placement): CXString {
    val _property = CString.fromString(property).getNativePtr().asLong()
    val _classUSR = classUSR.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_constructUSR_ObjCProperty(_property, _classUSR, _retValPlacement)
    free(NativePtr.byValue(_property))
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getCursorSpelling(arg0: CXCursor, retValPlacement: Placement): CXString {
    val _arg0 = arg0.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getCursorSpelling(_arg0, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_Cursor_getSpellingNameRange(arg0: CXCursor, pieceIndex: Int, options: Int, retValPlacement: Placement): CXSourceRange {
    val _arg0 = arg0.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceRange.size).asLong()
    val res = externals.clang_Cursor_getSpellingNameRange(_arg0, pieceIndex, options, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceRange)!!
}

fun clang_getCursorDisplayName(arg0: CXCursor, retValPlacement: Placement): CXString {
    val _arg0 = arg0.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getCursorDisplayName(_arg0, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getCursorReferenced(arg0: CXCursor, retValPlacement: Placement): CXCursor {
    val _arg0 = arg0.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXCursor.size).asLong()
    val res = externals.clang_getCursorReferenced(_arg0, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXCursor)!!
}

fun clang_getCursorDefinition(arg0: CXCursor, retValPlacement: Placement): CXCursor {
    val _arg0 = arg0.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXCursor.size).asLong()
    val res = externals.clang_getCursorDefinition(_arg0, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXCursor)!!
}

fun clang_isCursorDefinition(arg0: CXCursor): Int {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_isCursorDefinition(_arg0)
    return res
}

fun clang_getCanonicalCursor(arg0: CXCursor, retValPlacement: Placement): CXCursor {
    val _arg0 = arg0.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXCursor.size).asLong()
    val res = externals.clang_getCanonicalCursor(_arg0, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXCursor)!!
}

fun clang_Cursor_getObjCSelectorIndex(arg0: CXCursor): Int {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_Cursor_getObjCSelectorIndex(_arg0)
    return res
}

fun clang_Cursor_isDynamicCall(C: CXCursor): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_Cursor_isDynamicCall(_C)
    return res
}

fun clang_Cursor_getReceiverType(C: CXCursor, retValPlacement: Placement): CXType {
    val _C = C.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXType.size).asLong()
    val res = externals.clang_Cursor_getReceiverType(_C, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXType)!!
}

fun clang_Cursor_getObjCPropertyAttributes(C: CXCursor, reserved: Int): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_Cursor_getObjCPropertyAttributes(_C, reserved)
    return res
}

fun clang_Cursor_getObjCDeclQualifiers(C: CXCursor): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_Cursor_getObjCDeclQualifiers(_C)
    return res
}

fun clang_Cursor_isObjCOptional(C: CXCursor): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_Cursor_isObjCOptional(_C)
    return res
}

fun clang_Cursor_isVariadic(C: CXCursor): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_Cursor_isVariadic(_C)
    return res
}

fun clang_Cursor_getCommentRange(C: CXCursor, retValPlacement: Placement): CXSourceRange {
    val _C = C.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceRange.size).asLong()
    val res = externals.clang_Cursor_getCommentRange(_C, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceRange)!!
}

fun clang_Cursor_getRawCommentText(C: CXCursor, retValPlacement: Placement): CXString {
    val _C = C.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_Cursor_getRawCommentText(_C, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_Cursor_getBriefCommentText(C: CXCursor, retValPlacement: Placement): CXString {
    val _C = C.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_Cursor_getBriefCommentText(_C, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_Cursor_getMangling(arg0: CXCursor, retValPlacement: Placement): CXString {
    val _arg0 = arg0.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_Cursor_getMangling(_arg0, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_Cursor_getCXXManglings(arg0: CXCursor): CXStringSet? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_Cursor_getCXXManglings(_arg0)
    return NativePtr.byValue(res).asRef(CXStringSet)
}

fun clang_Cursor_getModule(C: CXCursor): NativePtr? {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_Cursor_getModule(_C)
    return NativePtr.byValue(res)
}

fun clang_getModuleForFile(arg0: CXTranslationUnitImpl?, arg1: NativePtr?): NativePtr? {
    val _arg0 = arg0.getNativePtr().asLong()
    val _arg1 = arg1.asLong()
    val res = externals.clang_getModuleForFile(_arg0, _arg1)
    return NativePtr.byValue(res)
}

fun clang_Module_getASTFile(Module: NativePtr?): NativePtr? {
    val _Module = Module.asLong()
    val res = externals.clang_Module_getASTFile(_Module)
    return NativePtr.byValue(res)
}

fun clang_Module_getParent(Module: NativePtr?): NativePtr? {
    val _Module = Module.asLong()
    val res = externals.clang_Module_getParent(_Module)
    return NativePtr.byValue(res)
}

fun clang_Module_getName(Module: NativePtr?, retValPlacement: Placement): CXString {
    val _Module = Module.asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_Module_getName(_Module, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_Module_getFullName(Module: NativePtr?, retValPlacement: Placement): CXString {
    val _Module = Module.asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_Module_getFullName(_Module, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_Module_isSystem(Module: NativePtr?): Int {
    val _Module = Module.asLong()
    val res = externals.clang_Module_isSystem(_Module)
    return res
}

fun clang_Module_getNumTopLevelHeaders(arg0: CXTranslationUnitImpl?, Module: NativePtr?): Int {
    val _arg0 = arg0.getNativePtr().asLong()
    val _Module = Module.asLong()
    val res = externals.clang_Module_getNumTopLevelHeaders(_arg0, _Module)
    return res
}

fun clang_Module_getTopLevelHeader(arg0: CXTranslationUnitImpl?, Module: NativePtr?, Index: Int): NativePtr? {
    val _arg0 = arg0.getNativePtr().asLong()
    val _Module = Module.asLong()
    val res = externals.clang_Module_getTopLevelHeader(_arg0, _Module, Index)
    return NativePtr.byValue(res)
}

fun clang_CXXField_isMutable(C: CXCursor): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_CXXField_isMutable(_C)
    return res
}

fun clang_CXXMethod_isPureVirtual(C: CXCursor): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_CXXMethod_isPureVirtual(_C)
    return res
}

fun clang_CXXMethod_isStatic(C: CXCursor): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_CXXMethod_isStatic(_C)
    return res
}

fun clang_CXXMethod_isVirtual(C: CXCursor): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_CXXMethod_isVirtual(_C)
    return res
}

fun clang_CXXMethod_isConst(C: CXCursor): Int {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_CXXMethod_isConst(_C)
    return res
}

fun clang_getTemplateCursorKind(C: CXCursor): CXCursorKind {
    val _C = C.getNativePtr().asLong()
    val res = externals.clang_getTemplateCursorKind(_C)
    return CXCursorKind.byValue(res)
}

fun clang_getSpecializedCursorTemplate(C: CXCursor, retValPlacement: Placement): CXCursor {
    val _C = C.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXCursor.size).asLong()
    val res = externals.clang_getSpecializedCursorTemplate(_C, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXCursor)!!
}

fun clang_getCursorReferenceNameRange(C: CXCursor, NameFlags: Int, PieceIndex: Int, retValPlacement: Placement): CXSourceRange {
    val _C = C.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceRange.size).asLong()
    val res = externals.clang_getCursorReferenceNameRange(_C, NameFlags, PieceIndex, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceRange)!!
}

fun clang_getTokenKind(arg0: CXToken): CXTokenKind {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_getTokenKind(_arg0)
    return CXTokenKind.byValue(res)
}

fun clang_getTokenSpelling(arg0: CXTranslationUnitImpl?, arg1: CXToken, retValPlacement: Placement): CXString {
    val _arg0 = arg0.getNativePtr().asLong()
    val _arg1 = arg1.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getTokenSpelling(_arg0, _arg1, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getTokenLocation(arg0: CXTranslationUnitImpl?, arg1: CXToken, retValPlacement: Placement): CXSourceLocation {
    val _arg0 = arg0.getNativePtr().asLong()
    val _arg1 = arg1.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceLocation.size).asLong()
    val res = externals.clang_getTokenLocation(_arg0, _arg1, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceLocation)!!
}

fun clang_getTokenExtent(arg0: CXTranslationUnitImpl?, arg1: CXToken, retValPlacement: Placement): CXSourceRange {
    val _arg0 = arg0.getNativePtr().asLong()
    val _arg1 = arg1.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceRange.size).asLong()
    val res = externals.clang_getTokenExtent(_arg0, _arg1, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceRange)!!
}

fun clang_tokenize(TU: CXTranslationUnitImpl?, Range: CXSourceRange, Tokens: RefBox<CXToken>?, NumTokens: Int32Box?): Unit {
    val _TU = TU.getNativePtr().asLong()
    val _Range = Range.getNativePtr().asLong()
    val _Tokens = Tokens.getNativePtr().asLong()
    val _NumTokens = NumTokens.getNativePtr().asLong()
    val res = externals.clang_tokenize(_TU, _Range, _Tokens, _NumTokens)
    return res
}

fun clang_annotateTokens(TU: CXTranslationUnitImpl?, Tokens: CXToken?, NumTokens: Int, Cursors: CXCursor?): Unit {
    val _TU = TU.getNativePtr().asLong()
    val _Tokens = Tokens.getNativePtr().asLong()
    val _Cursors = Cursors.getNativePtr().asLong()
    val res = externals.clang_annotateTokens(_TU, _Tokens, NumTokens, _Cursors)
    return res
}

fun clang_disposeTokens(TU: CXTranslationUnitImpl?, Tokens: CXToken?, NumTokens: Int): Unit {
    val _TU = TU.getNativePtr().asLong()
    val _Tokens = Tokens.getNativePtr().asLong()
    val res = externals.clang_disposeTokens(_TU, _Tokens, NumTokens)
    return res
}

fun clang_getCursorKindSpelling(Kind: CXCursorKind, retValPlacement: Placement): CXString {
    val _Kind = Kind.value
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getCursorKindSpelling(_Kind, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getDefinitionSpellingAndExtent(arg0: CXCursor, startBuf: RefBox<Int8Box>?, endBuf: RefBox<Int8Box>?, startLine: Int32Box?, startColumn: Int32Box?, endLine: Int32Box?, endColumn: Int32Box?): Unit {
    val _arg0 = arg0.getNativePtr().asLong()
    val _startBuf = startBuf.getNativePtr().asLong()
    val _endBuf = endBuf.getNativePtr().asLong()
    val _startLine = startLine.getNativePtr().asLong()
    val _startColumn = startColumn.getNativePtr().asLong()
    val _endLine = endLine.getNativePtr().asLong()
    val _endColumn = endColumn.getNativePtr().asLong()
    val res = externals.clang_getDefinitionSpellingAndExtent(_arg0, _startBuf, _endBuf, _startLine, _startColumn, _endLine, _endColumn)
    return res
}

fun clang_enableStackTraces(): Unit {
    val res = externals.clang_enableStackTraces()
    return res
}

fun clang_executeOnThread(fn: NativePtr?, user_data: NativePtr?, stack_size: Int): Unit {
    val _fn = fn.asLong()
    val _user_data = user_data.asLong()
    val res = externals.clang_executeOnThread(_fn, _user_data, stack_size)
    return res
}

fun clang_getCompletionChunkKind(completion_string: NativePtr?, chunk_number: Int): CXCompletionChunkKind {
    val _completion_string = completion_string.asLong()
    val res = externals.clang_getCompletionChunkKind(_completion_string, chunk_number)
    return CXCompletionChunkKind.byValue(res)
}

fun clang_getCompletionChunkText(completion_string: NativePtr?, chunk_number: Int, retValPlacement: Placement): CXString {
    val _completion_string = completion_string.asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getCompletionChunkText(_completion_string, chunk_number, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getCompletionChunkCompletionString(completion_string: NativePtr?, chunk_number: Int): NativePtr? {
    val _completion_string = completion_string.asLong()
    val res = externals.clang_getCompletionChunkCompletionString(_completion_string, chunk_number)
    return NativePtr.byValue(res)
}

fun clang_getNumCompletionChunks(completion_string: NativePtr?): Int {
    val _completion_string = completion_string.asLong()
    val res = externals.clang_getNumCompletionChunks(_completion_string)
    return res
}

fun clang_getCompletionPriority(completion_string: NativePtr?): Int {
    val _completion_string = completion_string.asLong()
    val res = externals.clang_getCompletionPriority(_completion_string)
    return res
}

fun clang_getCompletionAvailability(completion_string: NativePtr?): CXAvailabilityKind {
    val _completion_string = completion_string.asLong()
    val res = externals.clang_getCompletionAvailability(_completion_string)
    return CXAvailabilityKind.byValue(res)
}

fun clang_getCompletionNumAnnotations(completion_string: NativePtr?): Int {
    val _completion_string = completion_string.asLong()
    val res = externals.clang_getCompletionNumAnnotations(_completion_string)
    return res
}

fun clang_getCompletionAnnotation(completion_string: NativePtr?, annotation_number: Int, retValPlacement: Placement): CXString {
    val _completion_string = completion_string.asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getCompletionAnnotation(_completion_string, annotation_number, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getCompletionParent(completion_string: NativePtr?, kind: CXCursorKind.ref?, retValPlacement: Placement): CXString {
    val _completion_string = completion_string.asLong()
    val _kind = kind.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getCompletionParent(_completion_string, _kind, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getCompletionBriefComment(completion_string: NativePtr?, retValPlacement: Placement): CXString {
    val _completion_string = completion_string.asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getCompletionBriefComment(_completion_string, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getCursorCompletionString(cursor: CXCursor): NativePtr? {
    val _cursor = cursor.getNativePtr().asLong()
    val res = externals.clang_getCursorCompletionString(_cursor)
    return NativePtr.byValue(res)
}

fun clang_defaultCodeCompleteOptions(): Int {
    val res = externals.clang_defaultCodeCompleteOptions()
    return res
}

fun clang_codeCompleteAt(TU: CXTranslationUnitImpl?, complete_filename: String?, complete_line: Int, complete_column: Int, unsaved_files: CXUnsavedFile?, num_unsaved_files: Int, options: Int): CXCodeCompleteResults? {
    val _TU = TU.getNativePtr().asLong()
    val _complete_filename = CString.fromString(complete_filename).getNativePtr().asLong()
    val _unsaved_files = unsaved_files.getNativePtr().asLong()
    val res = externals.clang_codeCompleteAt(_TU, _complete_filename, complete_line, complete_column, _unsaved_files, num_unsaved_files, options)
    free(NativePtr.byValue(_complete_filename))
    return NativePtr.byValue(res).asRef(CXCodeCompleteResults)
}

fun clang_sortCodeCompletionResults(Results: CXCompletionResult?, NumResults: Int): Unit {
    val _Results = Results.getNativePtr().asLong()
    val res = externals.clang_sortCodeCompletionResults(_Results, NumResults)
    return res
}

fun clang_disposeCodeCompleteResults(Results: CXCodeCompleteResults?): Unit {
    val _Results = Results.getNativePtr().asLong()
    val res = externals.clang_disposeCodeCompleteResults(_Results)
    return res
}

fun clang_codeCompleteGetNumDiagnostics(Results: CXCodeCompleteResults?): Int {
    val _Results = Results.getNativePtr().asLong()
    val res = externals.clang_codeCompleteGetNumDiagnostics(_Results)
    return res
}

fun clang_codeCompleteGetDiagnostic(Results: CXCodeCompleteResults?, Index: Int): NativePtr? {
    val _Results = Results.getNativePtr().asLong()
    val res = externals.clang_codeCompleteGetDiagnostic(_Results, Index)
    return NativePtr.byValue(res)
}

fun clang_codeCompleteGetContexts(Results: CXCodeCompleteResults?): Long {
    val _Results = Results.getNativePtr().asLong()
    val res = externals.clang_codeCompleteGetContexts(_Results)
    return res
}

fun clang_codeCompleteGetContainerKind(Results: CXCodeCompleteResults?, IsIncomplete: Int32Box?): CXCursorKind {
    val _Results = Results.getNativePtr().asLong()
    val _IsIncomplete = IsIncomplete.getNativePtr().asLong()
    val res = externals.clang_codeCompleteGetContainerKind(_Results, _IsIncomplete)
    return CXCursorKind.byValue(res)
}

fun clang_codeCompleteGetContainerUSR(Results: CXCodeCompleteResults?, retValPlacement: Placement): CXString {
    val _Results = Results.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_codeCompleteGetContainerUSR(_Results, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_codeCompleteGetObjCSelector(Results: CXCodeCompleteResults?, retValPlacement: Placement): CXString {
    val _Results = Results.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_codeCompleteGetObjCSelector(_Results, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_getClangVersion(retValPlacement: Placement): CXString {
    val _retValPlacement = retValPlacement.alloc(CXString.size).asLong()
    val res = externals.clang_getClangVersion(_retValPlacement)
    return NativePtr.byValue(res).asRef(CXString)!!
}

fun clang_toggleCrashRecovery(isEnabled: Int): Unit {
    val res = externals.clang_toggleCrashRecovery(isEnabled)
    return res
}

fun clang_getInclusions(tu: CXTranslationUnitImpl?, visitor: NativePtr?, client_data: NativePtr?): Unit {
    val _tu = tu.getNativePtr().asLong()
    val _visitor = visitor.asLong()
    val _client_data = client_data.asLong()
    val res = externals.clang_getInclusions(_tu, _visitor, _client_data)
    return res
}

fun clang_getRemappings(path: String?): NativePtr? {
    val _path = CString.fromString(path).getNativePtr().asLong()
    val res = externals.clang_getRemappings(_path)
    free(NativePtr.byValue(_path))
    return NativePtr.byValue(res)
}

fun clang_getRemappingsFromFileList(filePaths: RefBox<Int8Box>?, numFiles: Int): NativePtr? {
    val _filePaths = filePaths.getNativePtr().asLong()
    val res = externals.clang_getRemappingsFromFileList(_filePaths, numFiles)
    return NativePtr.byValue(res)
}

fun clang_remap_getNumFiles(arg0: NativePtr?): Int {
    val _arg0 = arg0.asLong()
    val res = externals.clang_remap_getNumFiles(_arg0)
    return res
}

fun clang_remap_getFilenames(arg0: NativePtr?, index: Int, original: CXString?, transformed: CXString?): Unit {
    val _arg0 = arg0.asLong()
    val _original = original.getNativePtr().asLong()
    val _transformed = transformed.getNativePtr().asLong()
    val res = externals.clang_remap_getFilenames(_arg0, index, _original, _transformed)
    return res
}

fun clang_remap_dispose(arg0: NativePtr?): Unit {
    val _arg0 = arg0.asLong()
    val res = externals.clang_remap_dispose(_arg0)
    return res
}

fun clang_findReferencesInFile(cursor: CXCursor, file: NativePtr?, visitor: CXCursorAndRangeVisitor): CXResult {
    val _cursor = cursor.getNativePtr().asLong()
    val _file = file.asLong()
    val _visitor = visitor.getNativePtr().asLong()
    val res = externals.clang_findReferencesInFile(_cursor, _file, _visitor)
    return CXResult.byValue(res)
}

fun clang_findIncludesInFile(TU: CXTranslationUnitImpl?, file: NativePtr?, visitor: CXCursorAndRangeVisitor): CXResult {
    val _TU = TU.getNativePtr().asLong()
    val _file = file.asLong()
    val _visitor = visitor.getNativePtr().asLong()
    val res = externals.clang_findIncludesInFile(_TU, _file, _visitor)
    return CXResult.byValue(res)
}

fun clang_index_isEntityObjCContainerKind(arg0: CXIdxEntityKind): Int {
    val _arg0 = arg0.value
    val res = externals.clang_index_isEntityObjCContainerKind(_arg0)
    return res
}

fun clang_index_getObjCContainerDeclInfo(arg0: CXIdxDeclInfo?): CXIdxObjCContainerDeclInfo? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_index_getObjCContainerDeclInfo(_arg0)
    return NativePtr.byValue(res).asRef(CXIdxObjCContainerDeclInfo)
}

fun clang_index_getObjCInterfaceDeclInfo(arg0: CXIdxDeclInfo?): CXIdxObjCInterfaceDeclInfo? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_index_getObjCInterfaceDeclInfo(_arg0)
    return NativePtr.byValue(res).asRef(CXIdxObjCInterfaceDeclInfo)
}

fun clang_index_getObjCCategoryDeclInfo(arg0: CXIdxDeclInfo?): CXIdxObjCCategoryDeclInfo? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_index_getObjCCategoryDeclInfo(_arg0)
    return NativePtr.byValue(res).asRef(CXIdxObjCCategoryDeclInfo)
}

fun clang_index_getObjCProtocolRefListInfo(arg0: CXIdxDeclInfo?): CXIdxObjCProtocolRefListInfo? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_index_getObjCProtocolRefListInfo(_arg0)
    return NativePtr.byValue(res).asRef(CXIdxObjCProtocolRefListInfo)
}

fun clang_index_getObjCPropertyDeclInfo(arg0: CXIdxDeclInfo?): CXIdxObjCPropertyDeclInfo? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_index_getObjCPropertyDeclInfo(_arg0)
    return NativePtr.byValue(res).asRef(CXIdxObjCPropertyDeclInfo)
}

fun clang_index_getIBOutletCollectionAttrInfo(arg0: CXIdxAttrInfo?): CXIdxIBOutletCollectionAttrInfo? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_index_getIBOutletCollectionAttrInfo(_arg0)
    return NativePtr.byValue(res).asRef(CXIdxIBOutletCollectionAttrInfo)
}

fun clang_index_getCXXClassDeclInfo(arg0: CXIdxDeclInfo?): CXIdxCXXClassDeclInfo? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_index_getCXXClassDeclInfo(_arg0)
    return NativePtr.byValue(res).asRef(CXIdxCXXClassDeclInfo)
}

fun clang_index_getClientContainer(arg0: CXIdxContainerInfo?): NativePtr? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_index_getClientContainer(_arg0)
    return NativePtr.byValue(res)
}

fun clang_index_setClientContainer(arg0: CXIdxContainerInfo?, arg1: NativePtr?): Unit {
    val _arg0 = arg0.getNativePtr().asLong()
    val _arg1 = arg1.asLong()
    val res = externals.clang_index_setClientContainer(_arg0, _arg1)
    return res
}

fun clang_index_getClientEntity(arg0: CXIdxEntityInfo?): NativePtr? {
    val _arg0 = arg0.getNativePtr().asLong()
    val res = externals.clang_index_getClientEntity(_arg0)
    return NativePtr.byValue(res)
}

fun clang_index_setClientEntity(arg0: CXIdxEntityInfo?, arg1: NativePtr?): Unit {
    val _arg0 = arg0.getNativePtr().asLong()
    val _arg1 = arg1.asLong()
    val res = externals.clang_index_setClientEntity(_arg0, _arg1)
    return res
}

fun clang_IndexAction_create(CIdx: NativePtr?): NativePtr? {
    val _CIdx = CIdx.asLong()
    val res = externals.clang_IndexAction_create(_CIdx)
    return NativePtr.byValue(res)
}

fun clang_IndexAction_dispose(arg0: NativePtr?): Unit {
    val _arg0 = arg0.asLong()
    val res = externals.clang_IndexAction_dispose(_arg0)
    return res
}

fun clang_indexSourceFile(arg0: NativePtr?, client_data: NativePtr?, index_callbacks: IndexerCallbacks?, index_callbacks_size: Int, index_options: Int, source_filename: String?, command_line_args: RefBox<Int8Box>?, num_command_line_args: Int, unsaved_files: CXUnsavedFile?, num_unsaved_files: Int, out_TU: RefBox<CXTranslationUnitImpl>?, TU_options: Int): Int {
    val _arg0 = arg0.asLong()
    val _client_data = client_data.asLong()
    val _index_callbacks = index_callbacks.getNativePtr().asLong()
    val _source_filename = CString.fromString(source_filename).getNativePtr().asLong()
    val _command_line_args = command_line_args.getNativePtr().asLong()
    val _unsaved_files = unsaved_files.getNativePtr().asLong()
    val _out_TU = out_TU.getNativePtr().asLong()
    val res = externals.clang_indexSourceFile(_arg0, _client_data, _index_callbacks, index_callbacks_size, index_options, _source_filename, _command_line_args, num_command_line_args, _unsaved_files, num_unsaved_files, _out_TU, TU_options)
    free(NativePtr.byValue(_source_filename))
    return res
}

fun clang_indexSourceFileFullArgv(arg0: NativePtr?, client_data: NativePtr?, index_callbacks: IndexerCallbacks?, index_callbacks_size: Int, index_options: Int, source_filename: String?, command_line_args: RefBox<Int8Box>?, num_command_line_args: Int, unsaved_files: CXUnsavedFile?, num_unsaved_files: Int, out_TU: RefBox<CXTranslationUnitImpl>?, TU_options: Int): Int {
    val _arg0 = arg0.asLong()
    val _client_data = client_data.asLong()
    val _index_callbacks = index_callbacks.getNativePtr().asLong()
    val _source_filename = CString.fromString(source_filename).getNativePtr().asLong()
    val _command_line_args = command_line_args.getNativePtr().asLong()
    val _unsaved_files = unsaved_files.getNativePtr().asLong()
    val _out_TU = out_TU.getNativePtr().asLong()
    val res = externals.clang_indexSourceFileFullArgv(_arg0, _client_data, _index_callbacks, index_callbacks_size, index_options, _source_filename, _command_line_args, num_command_line_args, _unsaved_files, num_unsaved_files, _out_TU, TU_options)
    free(NativePtr.byValue(_source_filename))
    return res
}

fun clang_indexTranslationUnit(arg0: NativePtr?, client_data: NativePtr?, index_callbacks: IndexerCallbacks?, index_callbacks_size: Int, index_options: Int, arg5: CXTranslationUnitImpl?): Int {
    val _arg0 = arg0.asLong()
    val _client_data = client_data.asLong()
    val _index_callbacks = index_callbacks.getNativePtr().asLong()
    val _arg5 = arg5.getNativePtr().asLong()
    val res = externals.clang_indexTranslationUnit(_arg0, _client_data, _index_callbacks, index_callbacks_size, index_options, _arg5)
    return res
}

fun clang_indexLoc_getFileLocation(loc: CXIdxLoc, indexFile: NativePtrBox?, file: NativePtrBox?, line: Int32Box?, column: Int32Box?, offset: Int32Box?): Unit {
    val _loc = loc.getNativePtr().asLong()
    val _indexFile = indexFile.getNativePtr().asLong()
    val _file = file.getNativePtr().asLong()
    val _line = line.getNativePtr().asLong()
    val _column = column.getNativePtr().asLong()
    val _offset = offset.getNativePtr().asLong()
    val res = externals.clang_indexLoc_getFileLocation(_loc, _indexFile, _file, _line, _column, _offset)
    return res
}

fun clang_indexLoc_getCXSourceLocation(loc: CXIdxLoc, retValPlacement: Placement): CXSourceLocation {
    val _loc = loc.getNativePtr().asLong()
    val _retValPlacement = retValPlacement.alloc(CXSourceLocation.size).asLong()
    val res = externals.clang_indexLoc_getCXSourceLocation(_loc, _retValPlacement)
    return NativePtr.byValue(res).asRef(CXSourceLocation)!!
}

fun clang_Type_visitFields(T: CXType, visitor: NativePtr?, client_data: NativePtr?): Int {
    val _T = T.getNativePtr().asLong()
    val _visitor = visitor.asLong()
    val _client_data = client_data.asLong()
    val res = externals.clang_Type_visitFields(_T, _visitor, _client_data)
    return res
}

class __mbstate_t(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<__mbstate_t>(128, ::__mbstate_t)
    
    val __mbstate8 by array[128](Int8Box) at 0
    val _mbstateL by Int64Box at 0
}

class __darwin_pthread_handler_rec(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<__darwin_pthread_handler_rec>(24, ::__darwin_pthread_handler_rec)
    
    val __routine by NativePtrBox at 0
    val __arg by NativePtrBox at 8
    val __next by __darwin_pthread_handler_rec.ref at 16
}

class _opaque_pthread_attr_t(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<_opaque_pthread_attr_t>(64, ::_opaque_pthread_attr_t)
    
    val __sig by Int64Box at 0
    val __opaque by array[56](Int8Box) at 8
}

class _opaque_pthread_cond_t(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<_opaque_pthread_cond_t>(48, ::_opaque_pthread_cond_t)
    
    val __sig by Int64Box at 0
    val __opaque by array[40](Int8Box) at 8
}

class _opaque_pthread_condattr_t(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<_opaque_pthread_condattr_t>(16, ::_opaque_pthread_condattr_t)
    
    val __sig by Int64Box at 0
    val __opaque by array[8](Int8Box) at 8
}

class _opaque_pthread_mutex_t(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<_opaque_pthread_mutex_t>(64, ::_opaque_pthread_mutex_t)
    
    val __sig by Int64Box at 0
    val __opaque by array[56](Int8Box) at 8
}

class _opaque_pthread_mutexattr_t(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<_opaque_pthread_mutexattr_t>(16, ::_opaque_pthread_mutexattr_t)
    
    val __sig by Int64Box at 0
    val __opaque by array[8](Int8Box) at 8
}

class _opaque_pthread_once_t(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<_opaque_pthread_once_t>(16, ::_opaque_pthread_once_t)
    
    val __sig by Int64Box at 0
    val __opaque by array[8](Int8Box) at 8
}

class _opaque_pthread_rwlock_t(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<_opaque_pthread_rwlock_t>(200, ::_opaque_pthread_rwlock_t)
    
    val __sig by Int64Box at 0
    val __opaque by array[192](Int8Box) at 8
}

class _opaque_pthread_rwlockattr_t(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<_opaque_pthread_rwlockattr_t>(24, ::_opaque_pthread_rwlockattr_t)
    
    val __sig by Int64Box at 0
    val __opaque by array[16](Int8Box) at 8
}

class _opaque_pthread_t(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<_opaque_pthread_t>(8192, ::_opaque_pthread_t)
    
    val __sig by Int64Box at 0
    val __cleanup_stack by __darwin_pthread_handler_rec.ref at 8
    val __opaque by array[8176](Int8Box) at 16
}

class timespec(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<timespec>(16, ::timespec)
    
    val tv_sec by Int64Box at 0
    val tv_nsec by Int64Box at 8
}

class tm(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<tm>(56, ::tm)
    
    val tm_sec by Int32Box at 0
    val tm_min by Int32Box at 4
    val tm_hour by Int32Box at 8
    val tm_mday by Int32Box at 12
    val tm_mon by Int32Box at 16
    val tm_year by Int32Box at 20
    val tm_wday by Int32Box at 24
    val tm_yday by Int32Box at 28
    val tm_isdst by Int32Box at 32
    val tm_gmtoff by Int64Box at 40
    val tm_zone by Int8Box.ref at 48
}

class CXString(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXString>(16, ::CXString)
    
    val data by NativePtrBox at 0
    val private_flags by Int32Box at 8
}

class CXStringSet(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXStringSet>(16, ::CXStringSet)
    
    val Strings by CXString.ref at 0
    val Count by Int32Box at 8
}

class CXVirtualFileOverlayImpl(ptr: NativePtr) : NativeRef(ptr) {
    companion object : Type<CXVirtualFileOverlayImpl>(::CXVirtualFileOverlayImpl)
}

class CXModuleMapDescriptorImpl(ptr: NativePtr) : NativeRef(ptr) {
    companion object : Type<CXModuleMapDescriptorImpl>(::CXModuleMapDescriptorImpl)
}

class CXTranslationUnitImpl(ptr: NativePtr) : NativeRef(ptr) {
    companion object : Type<CXTranslationUnitImpl>(::CXTranslationUnitImpl)
}

class CXUnsavedFile(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXUnsavedFile>(24, ::CXUnsavedFile)
    
    val Filename by Int8Box.ref at 0
    val Contents by Int8Box.ref at 8
    val Length by Int64Box at 16
}

class CXVersion(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXVersion>(12, ::CXVersion)
    
    val Major by Int32Box at 0
    val Minor by Int32Box at 4
    val Subminor by Int32Box at 8
}

class CXFileUniqueID(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXFileUniqueID>(24, ::CXFileUniqueID)
    
    val data by array[3](Int64Box) at 0
}

class CXSourceLocation(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXSourceLocation>(24, ::CXSourceLocation)
    
    val ptr_data by array[2](NativePtrBox) at 0
    val int_data by Int32Box at 16
}

class CXSourceRange(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXSourceRange>(24, ::CXSourceRange)
    
    val ptr_data by array[2](NativePtrBox) at 0
    val begin_int_data by Int32Box at 16
    val end_int_data by Int32Box at 20
}

class CXSourceRangeList(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXSourceRangeList>(16, ::CXSourceRangeList)
    
    val count by Int32Box at 0
    val ranges by CXSourceRange.ref at 8
}

class CXTUResourceUsageEntry(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXTUResourceUsageEntry>(16, ::CXTUResourceUsageEntry)
    
    val kind by CXTUResourceUsageKind.ref at 0
    val amount by Int64Box at 8
}

class CXTUResourceUsage(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXTUResourceUsage>(24, ::CXTUResourceUsage)
    
    val data by NativePtrBox at 0
    val numEntries by Int32Box at 8
    val entries by CXTUResourceUsageEntry.ref at 16
}

class CXCursor(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXCursor>(32, ::CXCursor)
    
    val kind by CXCursorKind.ref at 0
    val xdata by Int32Box at 4
    val data by array[3](NativePtrBox) at 8
}

class CXPlatformAvailability(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXPlatformAvailability>(72, ::CXPlatformAvailability)
    
    val Platform by CXString at 0
    val Introduced by CXVersion at 16
    val Deprecated by CXVersion at 28
    val Obsoleted by CXVersion at 40
    val Unavailable by Int32Box at 52
    val Message by CXString at 56
}

class CXCursorSetImpl(ptr: NativePtr) : NativeRef(ptr) {
    companion object : Type<CXCursorSetImpl>(::CXCursorSetImpl)
}

class CXType(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXType>(24, ::CXType)
    
    val kind by CXTypeKind.ref at 0
    val data by array[2](NativePtrBox) at 8
}

class CXToken(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXToken>(24, ::CXToken)
    
    val int_data by array[4](Int32Box) at 0
    val ptr_data by NativePtrBox at 16
}

class CXCompletionResult(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXCompletionResult>(16, ::CXCompletionResult)
    
    val CursorKind by CXCursorKind.ref at 0
    val CompletionString by NativePtrBox at 8
}

class CXCodeCompleteResults(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXCodeCompleteResults>(16, ::CXCodeCompleteResults)
    
    val Results by CXCompletionResult.ref at 0
    val NumResults by Int32Box at 8
}

class CXCursorAndRangeVisitor(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXCursorAndRangeVisitor>(16, ::CXCursorAndRangeVisitor)
    
    val context by NativePtrBox at 0
    val visit by NativePtrBox at 8
}

class CXIdxLoc(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxLoc>(24, ::CXIdxLoc)
    
    val ptr_data by array[2](NativePtrBox) at 0
    val int_data by Int32Box at 16
}

class CXIdxIncludedFileInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxIncludedFileInfo>(56, ::CXIdxIncludedFileInfo)
    
    val hashLoc by CXIdxLoc at 0
    val filename by Int8Box.ref at 24
    val file by NativePtrBox at 32
    val isImport by Int32Box at 40
    val isAngled by Int32Box at 44
    val isModuleImport by Int32Box at 48
}

class CXIdxImportedASTFileInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxImportedASTFileInfo>(48, ::CXIdxImportedASTFileInfo)
    
    val file by NativePtrBox at 0
    val module by NativePtrBox at 8
    val loc by CXIdxLoc at 16
    val isImplicit by Int32Box at 40
}

class CXIdxAttrInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxAttrInfo>(64, ::CXIdxAttrInfo)
    
    val kind by CXIdxAttrKind.ref at 0
    val cursor by CXCursor at 8
    val loc by CXIdxLoc at 40
}

class CXIdxEntityInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxEntityInfo>(80, ::CXIdxEntityInfo)
    
    val kind by CXIdxEntityKind.ref at 0
    val templateKind by CXIdxEntityCXXTemplateKind.ref at 4
    val lang by CXIdxEntityLanguage.ref at 8
    val name by Int8Box.ref at 16
    val USR by Int8Box.ref at 24
    val cursor by CXCursor at 32
    val attributes by CXIdxAttrInfo.ref.ref at 64
    val numAttributes by Int32Box at 72
}

class CXIdxContainerInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxContainerInfo>(32, ::CXIdxContainerInfo)
    
    val cursor by CXCursor at 0
}

class CXIdxIBOutletCollectionAttrInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxIBOutletCollectionAttrInfo>(72, ::CXIdxIBOutletCollectionAttrInfo)
    
    val attrInfo by CXIdxAttrInfo.ref at 0
    val objcClass by CXIdxEntityInfo.ref at 8
    val classCursor by CXCursor at 16
    val classLoc by CXIdxLoc at 48
}

class CXIdxDeclInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxDeclInfo>(128, ::CXIdxDeclInfo)
    
    val entityInfo by CXIdxEntityInfo.ref at 0
    val cursor by CXCursor at 8
    val loc by CXIdxLoc at 40
    val semanticContainer by CXIdxContainerInfo.ref at 64
    val lexicalContainer by CXIdxContainerInfo.ref at 72
    val isRedeclaration by Int32Box at 80
    val isDefinition by Int32Box at 84
    val isContainer by Int32Box at 88
    val declAsContainer by CXIdxContainerInfo.ref at 96
    val isImplicit by Int32Box at 104
    val attributes by CXIdxAttrInfo.ref.ref at 112
    val numAttributes by Int32Box at 120
    val flags by Int32Box at 124
}

class CXIdxObjCContainerDeclInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxObjCContainerDeclInfo>(16, ::CXIdxObjCContainerDeclInfo)
    
    val declInfo by CXIdxDeclInfo.ref at 0
    val kind by CXIdxObjCContainerKind.ref at 8
}

class CXIdxBaseClassInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxBaseClassInfo>(64, ::CXIdxBaseClassInfo)
    
    val base by CXIdxEntityInfo.ref at 0
    val cursor by CXCursor at 8
    val loc by CXIdxLoc at 40
}

class CXIdxObjCProtocolRefInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxObjCProtocolRefInfo>(64, ::CXIdxObjCProtocolRefInfo)
    
    val protocol by CXIdxEntityInfo.ref at 0
    val cursor by CXCursor at 8
    val loc by CXIdxLoc at 40
}

class CXIdxObjCProtocolRefListInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxObjCProtocolRefListInfo>(16, ::CXIdxObjCProtocolRefListInfo)
    
    val protocols by CXIdxObjCProtocolRefInfo.ref.ref at 0
    val numProtocols by Int32Box at 8
}

class CXIdxObjCInterfaceDeclInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxObjCInterfaceDeclInfo>(24, ::CXIdxObjCInterfaceDeclInfo)
    
    val containerInfo by CXIdxObjCContainerDeclInfo.ref at 0
    val superInfo by CXIdxBaseClassInfo.ref at 8
    val protocols by CXIdxObjCProtocolRefListInfo.ref at 16
}

class CXIdxObjCCategoryDeclInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxObjCCategoryDeclInfo>(80, ::CXIdxObjCCategoryDeclInfo)
    
    val containerInfo by CXIdxObjCContainerDeclInfo.ref at 0
    val objcClass by CXIdxEntityInfo.ref at 8
    val classCursor by CXCursor at 16
    val classLoc by CXIdxLoc at 48
    val protocols by CXIdxObjCProtocolRefListInfo.ref at 72
}

class CXIdxObjCPropertyDeclInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxObjCPropertyDeclInfo>(24, ::CXIdxObjCPropertyDeclInfo)
    
    val declInfo by CXIdxDeclInfo.ref at 0
    val getter by CXIdxEntityInfo.ref at 8
    val setter by CXIdxEntityInfo.ref at 16
}

class CXIdxCXXClassDeclInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxCXXClassDeclInfo>(24, ::CXIdxCXXClassDeclInfo)
    
    val declInfo by CXIdxDeclInfo.ref at 0
    val bases by CXIdxBaseClassInfo.ref.ref at 8
    val numBases by Int32Box at 16
}

class CXIdxEntityRefInfo(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<CXIdxEntityRefInfo>(88, ::CXIdxEntityRefInfo)
    
    val kind by CXIdxEntityRefKind.ref at 0
    val cursor by CXCursor at 8
    val loc by CXIdxLoc at 40
    val referencedEntity by CXIdxEntityInfo.ref at 64
    val parentEntity by CXIdxEntityInfo.ref at 72
    val container by CXIdxContainerInfo.ref at 80
}

class IndexerCallbacks(ptr: NativePtr) : NativeStruct(ptr) {
    
    companion object : Type<IndexerCallbacks>(64, ::IndexerCallbacks)
    
    val abortQuery by NativePtrBox at 0
    val diagnostic by NativePtrBox at 8
    val enteredMainFile by NativePtrBox at 16
    val ppIncludedFile by NativePtrBox at 24
    val importedASTFile by NativePtrBox at 32
    val startedTranslationUnit by NativePtrBox at 40
    val indexDeclaration by NativePtrBox at 48
    val indexEntityReference by NativePtrBox at 56
}

enum class CXErrorCode(val value: Int) {
    CXError_Success(0),
    CXError_Failure(1),
    CXError_Crashed(2),
    CXError_InvalidArguments(3),
    CXError_ASTReadError(4),
    ;
    
    companion object {
        fun byValue(value: Int) = CXErrorCode.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXErrorCode
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXAvailabilityKind(val value: Int) {
    CXAvailability_Available(0),
    CXAvailability_Deprecated(1),
    CXAvailability_NotAvailable(2),
    CXAvailability_NotAccessible(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CXAvailabilityKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXAvailabilityKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXGlobalOptFlags(val value: Int) {
    CXGlobalOpt_None(0),
    CXGlobalOpt_ThreadBackgroundPriorityForIndexing(1),
    CXGlobalOpt_ThreadBackgroundPriorityForEditing(2),
    CXGlobalOpt_ThreadBackgroundPriorityForAll(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CXGlobalOptFlags.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXGlobalOptFlags
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXDiagnosticSeverity(val value: Int) {
    CXDiagnostic_Ignored(0),
    CXDiagnostic_Note(1),
    CXDiagnostic_Warning(2),
    CXDiagnostic_Error(3),
    CXDiagnostic_Fatal(4),
    ;
    
    companion object {
        fun byValue(value: Int) = CXDiagnosticSeverity.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXDiagnosticSeverity
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXLoadDiag_Error(val value: Int) {
    CXLoadDiag_None(0),
    CXLoadDiag_Unknown(1),
    CXLoadDiag_CannotLoad(2),
    CXLoadDiag_InvalidFile(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CXLoadDiag_Error.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXLoadDiag_Error
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXDiagnosticDisplayOptions(val value: Int) {
    CXDiagnostic_DisplaySourceLocation(1),
    CXDiagnostic_DisplayColumn(2),
    CXDiagnostic_DisplaySourceRanges(4),
    CXDiagnostic_DisplayOption(8),
    CXDiagnostic_DisplayCategoryId(16),
    CXDiagnostic_DisplayCategoryName(32),
    ;
    
    companion object {
        fun byValue(value: Int) = CXDiagnosticDisplayOptions.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXDiagnosticDisplayOptions
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXTranslationUnit_Flags(val value: Int) {
    CXTranslationUnit_None(0),
    CXTranslationUnit_DetailedPreprocessingRecord(1),
    CXTranslationUnit_Incomplete(2),
    CXTranslationUnit_PrecompiledPreamble(4),
    CXTranslationUnit_CacheCompletionResults(8),
    CXTranslationUnit_ForSerialization(16),
    CXTranslationUnit_CXXChainedPCH(32),
    CXTranslationUnit_SkipFunctionBodies(64),
    CXTranslationUnit_IncludeBriefCommentsInCodeCompletion(128),
    CXTranslationUnit_CreatePreambleOnFirstParse(256),
    ;
    
    companion object {
        fun byValue(value: Int) = CXTranslationUnit_Flags.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXTranslationUnit_Flags
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXSaveTranslationUnit_Flags(val value: Int) {
    CXSaveTranslationUnit_None(0),
    ;
    
    companion object {
        fun byValue(value: Int) = CXSaveTranslationUnit_Flags.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXSaveTranslationUnit_Flags
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXSaveError(val value: Int) {
    CXSaveError_None(0),
    CXSaveError_Unknown(1),
    CXSaveError_TranslationErrors(2),
    CXSaveError_InvalidTU(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CXSaveError.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXSaveError
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXReparse_Flags(val value: Int) {
    CXReparse_None(0),
    ;
    
    companion object {
        fun byValue(value: Int) = CXReparse_Flags.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXReparse_Flags
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXTUResourceUsageKind(val value: Int) {
    CXTUResourceUsage_AST(1),
    CXTUResourceUsage_Identifiers(2),
    CXTUResourceUsage_Selectors(3),
    CXTUResourceUsage_GlobalCompletionResults(4),
    CXTUResourceUsage_SourceManagerContentCache(5),
    CXTUResourceUsage_AST_SideTables(6),
    CXTUResourceUsage_SourceManager_Membuffer_Malloc(7),
    CXTUResourceUsage_SourceManager_Membuffer_MMap(8),
    CXTUResourceUsage_ExternalASTSource_Membuffer_Malloc(9),
    CXTUResourceUsage_ExternalASTSource_Membuffer_MMap(10),
    CXTUResourceUsage_Preprocessor(11),
    CXTUResourceUsage_PreprocessingRecord(12),
    CXTUResourceUsage_SourceManager_DataStructures(13),
    CXTUResourceUsage_Preprocessor_HeaderSearch(14),
    CXTUResourceUsage_MEMORY_IN_BYTES_BEGIN(1),
    CXTUResourceUsage_MEMORY_IN_BYTES_END(14),
    CXTUResourceUsage_First(1),
    CXTUResourceUsage_Last(14),
    ;
    
    companion object {
        fun byValue(value: Int) = CXTUResourceUsageKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXTUResourceUsageKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXCursorKind(val value: Int) {
    CXCursor_UnexposedDecl(1),
    CXCursor_StructDecl(2),
    CXCursor_UnionDecl(3),
    CXCursor_ClassDecl(4),
    CXCursor_EnumDecl(5),
    CXCursor_FieldDecl(6),
    CXCursor_EnumConstantDecl(7),
    CXCursor_FunctionDecl(8),
    CXCursor_VarDecl(9),
    CXCursor_ParmDecl(10),
    CXCursor_ObjCInterfaceDecl(11),
    CXCursor_ObjCCategoryDecl(12),
    CXCursor_ObjCProtocolDecl(13),
    CXCursor_ObjCPropertyDecl(14),
    CXCursor_ObjCIvarDecl(15),
    CXCursor_ObjCInstanceMethodDecl(16),
    CXCursor_ObjCClassMethodDecl(17),
    CXCursor_ObjCImplementationDecl(18),
    CXCursor_ObjCCategoryImplDecl(19),
    CXCursor_TypedefDecl(20),
    CXCursor_CXXMethod(21),
    CXCursor_Namespace(22),
    CXCursor_LinkageSpec(23),
    CXCursor_Constructor(24),
    CXCursor_Destructor(25),
    CXCursor_ConversionFunction(26),
    CXCursor_TemplateTypeParameter(27),
    CXCursor_NonTypeTemplateParameter(28),
    CXCursor_TemplateTemplateParameter(29),
    CXCursor_FunctionTemplate(30),
    CXCursor_ClassTemplate(31),
    CXCursor_ClassTemplatePartialSpecialization(32),
    CXCursor_NamespaceAlias(33),
    CXCursor_UsingDirective(34),
    CXCursor_UsingDeclaration(35),
    CXCursor_TypeAliasDecl(36),
    CXCursor_ObjCSynthesizeDecl(37),
    CXCursor_ObjCDynamicDecl(38),
    CXCursor_CXXAccessSpecifier(39),
    CXCursor_FirstDecl(1),
    CXCursor_LastDecl(39),
    CXCursor_FirstRef(40),
    CXCursor_ObjCSuperClassRef(40),
    CXCursor_ObjCProtocolRef(41),
    CXCursor_ObjCClassRef(42),
    CXCursor_TypeRef(43),
    CXCursor_CXXBaseSpecifier(44),
    CXCursor_TemplateRef(45),
    CXCursor_NamespaceRef(46),
    CXCursor_MemberRef(47),
    CXCursor_LabelRef(48),
    CXCursor_OverloadedDeclRef(49),
    CXCursor_VariableRef(50),
    CXCursor_LastRef(50),
    CXCursor_FirstInvalid(70),
    CXCursor_InvalidFile(70),
    CXCursor_NoDeclFound(71),
    CXCursor_NotImplemented(72),
    CXCursor_InvalidCode(73),
    CXCursor_LastInvalid(73),
    CXCursor_FirstExpr(100),
    CXCursor_UnexposedExpr(100),
    CXCursor_DeclRefExpr(101),
    CXCursor_MemberRefExpr(102),
    CXCursor_CallExpr(103),
    CXCursor_ObjCMessageExpr(104),
    CXCursor_BlockExpr(105),
    CXCursor_IntegerLiteral(106),
    CXCursor_FloatingLiteral(107),
    CXCursor_ImaginaryLiteral(108),
    CXCursor_StringLiteral(109),
    CXCursor_CharacterLiteral(110),
    CXCursor_ParenExpr(111),
    CXCursor_UnaryOperator(112),
    CXCursor_ArraySubscriptExpr(113),
    CXCursor_BinaryOperator(114),
    CXCursor_CompoundAssignOperator(115),
    CXCursor_ConditionalOperator(116),
    CXCursor_CStyleCastExpr(117),
    CXCursor_CompoundLiteralExpr(118),
    CXCursor_InitListExpr(119),
    CXCursor_AddrLabelExpr(120),
    CXCursor_StmtExpr(121),
    CXCursor_GenericSelectionExpr(122),
    CXCursor_GNUNullExpr(123),
    CXCursor_CXXStaticCastExpr(124),
    CXCursor_CXXDynamicCastExpr(125),
    CXCursor_CXXReinterpretCastExpr(126),
    CXCursor_CXXConstCastExpr(127),
    CXCursor_CXXFunctionalCastExpr(128),
    CXCursor_CXXTypeidExpr(129),
    CXCursor_CXXBoolLiteralExpr(130),
    CXCursor_CXXNullPtrLiteralExpr(131),
    CXCursor_CXXThisExpr(132),
    CXCursor_CXXThrowExpr(133),
    CXCursor_CXXNewExpr(134),
    CXCursor_CXXDeleteExpr(135),
    CXCursor_UnaryExpr(136),
    CXCursor_ObjCStringLiteral(137),
    CXCursor_ObjCEncodeExpr(138),
    CXCursor_ObjCSelectorExpr(139),
    CXCursor_ObjCProtocolExpr(140),
    CXCursor_ObjCBridgedCastExpr(141),
    CXCursor_PackExpansionExpr(142),
    CXCursor_SizeOfPackExpr(143),
    CXCursor_LambdaExpr(144),
    CXCursor_ObjCBoolLiteralExpr(145),
    CXCursor_ObjCSelfExpr(146),
    CXCursor_OMPArraySectionExpr(147),
    CXCursor_LastExpr(147),
    CXCursor_FirstStmt(200),
    CXCursor_UnexposedStmt(200),
    CXCursor_LabelStmt(201),
    CXCursor_CompoundStmt(202),
    CXCursor_CaseStmt(203),
    CXCursor_DefaultStmt(204),
    CXCursor_IfStmt(205),
    CXCursor_SwitchStmt(206),
    CXCursor_WhileStmt(207),
    CXCursor_DoStmt(208),
    CXCursor_ForStmt(209),
    CXCursor_GotoStmt(210),
    CXCursor_IndirectGotoStmt(211),
    CXCursor_ContinueStmt(212),
    CXCursor_BreakStmt(213),
    CXCursor_ReturnStmt(214),
    CXCursor_GCCAsmStmt(215),
    CXCursor_AsmStmt(215),
    CXCursor_ObjCAtTryStmt(216),
    CXCursor_ObjCAtCatchStmt(217),
    CXCursor_ObjCAtFinallyStmt(218),
    CXCursor_ObjCAtThrowStmt(219),
    CXCursor_ObjCAtSynchronizedStmt(220),
    CXCursor_ObjCAutoreleasePoolStmt(221),
    CXCursor_ObjCForCollectionStmt(222),
    CXCursor_CXXCatchStmt(223),
    CXCursor_CXXTryStmt(224),
    CXCursor_CXXForRangeStmt(225),
    CXCursor_SEHTryStmt(226),
    CXCursor_SEHExceptStmt(227),
    CXCursor_SEHFinallyStmt(228),
    CXCursor_MSAsmStmt(229),
    CXCursor_NullStmt(230),
    CXCursor_DeclStmt(231),
    CXCursor_OMPParallelDirective(232),
    CXCursor_OMPSimdDirective(233),
    CXCursor_OMPForDirective(234),
    CXCursor_OMPSectionsDirective(235),
    CXCursor_OMPSectionDirective(236),
    CXCursor_OMPSingleDirective(237),
    CXCursor_OMPParallelForDirective(238),
    CXCursor_OMPParallelSectionsDirective(239),
    CXCursor_OMPTaskDirective(240),
    CXCursor_OMPMasterDirective(241),
    CXCursor_OMPCriticalDirective(242),
    CXCursor_OMPTaskyieldDirective(243),
    CXCursor_OMPBarrierDirective(244),
    CXCursor_OMPTaskwaitDirective(245),
    CXCursor_OMPFlushDirective(246),
    CXCursor_SEHLeaveStmt(247),
    CXCursor_OMPOrderedDirective(248),
    CXCursor_OMPAtomicDirective(249),
    CXCursor_OMPForSimdDirective(250),
    CXCursor_OMPParallelForSimdDirective(251),
    CXCursor_OMPTargetDirective(252),
    CXCursor_OMPTeamsDirective(253),
    CXCursor_OMPTaskgroupDirective(254),
    CXCursor_OMPCancellationPointDirective(255),
    CXCursor_OMPCancelDirective(256),
    CXCursor_OMPTargetDataDirective(257),
    CXCursor_OMPTaskLoopDirective(258),
    CXCursor_OMPTaskLoopSimdDirective(259),
    CXCursor_OMPDistributeDirective(260),
    CXCursor_LastStmt(260),
    CXCursor_TranslationUnit(300),
    CXCursor_FirstAttr(400),
    CXCursor_UnexposedAttr(400),
    CXCursor_IBActionAttr(401),
    CXCursor_IBOutletAttr(402),
    CXCursor_IBOutletCollectionAttr(403),
    CXCursor_CXXFinalAttr(404),
    CXCursor_CXXOverrideAttr(405),
    CXCursor_AnnotateAttr(406),
    CXCursor_AsmLabelAttr(407),
    CXCursor_PackedAttr(408),
    CXCursor_PureAttr(409),
    CXCursor_ConstAttr(410),
    CXCursor_NoDuplicateAttr(411),
    CXCursor_CUDAConstantAttr(412),
    CXCursor_CUDADeviceAttr(413),
    CXCursor_CUDAGlobalAttr(414),
    CXCursor_CUDAHostAttr(415),
    CXCursor_CUDASharedAttr(416),
    CXCursor_VisibilityAttr(417),
    CXCursor_DLLExport(418),
    CXCursor_DLLImport(419),
    CXCursor_LastAttr(419),
    CXCursor_PreprocessingDirective(500),
    CXCursor_MacroDefinition(501),
    CXCursor_MacroExpansion(502),
    CXCursor_MacroInstantiation(502),
    CXCursor_InclusionDirective(503),
    CXCursor_FirstPreprocessing(500),
    CXCursor_LastPreprocessing(503),
    CXCursor_ModuleImportDecl(600),
    CXCursor_TypeAliasTemplateDecl(601),
    CXCursor_FirstExtraDecl(600),
    CXCursor_LastExtraDecl(601),
    CXCursor_OverloadCandidate(700),
    ;
    
    companion object {
        fun byValue(value: Int) = CXCursorKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXCursorKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXLinkageKind(val value: Int) {
    CXLinkage_Invalid(0),
    CXLinkage_NoLinkage(1),
    CXLinkage_Internal(2),
    CXLinkage_UniqueExternal(3),
    CXLinkage_External(4),
    ;
    
    companion object {
        fun byValue(value: Int) = CXLinkageKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXLinkageKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXVisibilityKind(val value: Int) {
    CXVisibility_Invalid(0),
    CXVisibility_Hidden(1),
    CXVisibility_Protected(2),
    CXVisibility_Default(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CXVisibilityKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXVisibilityKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXLanguageKind(val value: Int) {
    CXLanguage_Invalid(0),
    CXLanguage_C(1),
    CXLanguage_ObjC(2),
    CXLanguage_CPlusPlus(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CXLanguageKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXLanguageKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXTypeKind(val value: Int) {
    CXType_Invalid(0),
    CXType_Unexposed(1),
    CXType_Void(2),
    CXType_Bool(3),
    CXType_Char_U(4),
    CXType_UChar(5),
    CXType_Char16(6),
    CXType_Char32(7),
    CXType_UShort(8),
    CXType_UInt(9),
    CXType_ULong(10),
    CXType_ULongLong(11),
    CXType_UInt128(12),
    CXType_Char_S(13),
    CXType_SChar(14),
    CXType_WChar(15),
    CXType_Short(16),
    CXType_Int(17),
    CXType_Long(18),
    CXType_LongLong(19),
    CXType_Int128(20),
    CXType_Float(21),
    CXType_Double(22),
    CXType_LongDouble(23),
    CXType_NullPtr(24),
    CXType_Overload(25),
    CXType_Dependent(26),
    CXType_ObjCId(27),
    CXType_ObjCClass(28),
    CXType_ObjCSel(29),
    CXType_FirstBuiltin(2),
    CXType_LastBuiltin(29),
    CXType_Complex(100),
    CXType_Pointer(101),
    CXType_BlockPointer(102),
    CXType_LValueReference(103),
    CXType_RValueReference(104),
    CXType_Record(105),
    CXType_Enum(106),
    CXType_Typedef(107),
    CXType_ObjCInterface(108),
    CXType_ObjCObjectPointer(109),
    CXType_FunctionNoProto(110),
    CXType_FunctionProto(111),
    CXType_ConstantArray(112),
    CXType_Vector(113),
    CXType_IncompleteArray(114),
    CXType_VariableArray(115),
    CXType_DependentSizedArray(116),
    CXType_MemberPointer(117),
    CXType_Auto(118),
    ;
    
    companion object {
        fun byValue(value: Int) = CXTypeKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXTypeKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXCallingConv(val value: Int) {
    CXCallingConv_Default(0),
    CXCallingConv_C(1),
    CXCallingConv_X86StdCall(2),
    CXCallingConv_X86FastCall(3),
    CXCallingConv_X86ThisCall(4),
    CXCallingConv_X86Pascal(5),
    CXCallingConv_AAPCS(6),
    CXCallingConv_AAPCS_VFP(7),
    CXCallingConv_IntelOclBicc(9),
    CXCallingConv_X86_64Win64(10),
    CXCallingConv_X86_64SysV(11),
    CXCallingConv_X86VectorCall(12),
    CXCallingConv_Invalid(100),
    CXCallingConv_Unexposed(200),
    ;
    
    companion object {
        fun byValue(value: Int) = CXCallingConv.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXCallingConv
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXTemplateArgumentKind(val value: Int) {
    CXTemplateArgumentKind_Null(0),
    CXTemplateArgumentKind_Type(1),
    CXTemplateArgumentKind_Declaration(2),
    CXTemplateArgumentKind_NullPtr(3),
    CXTemplateArgumentKind_Integral(4),
    CXTemplateArgumentKind_Template(5),
    CXTemplateArgumentKind_TemplateExpansion(6),
    CXTemplateArgumentKind_Expression(7),
    CXTemplateArgumentKind_Pack(8),
    CXTemplateArgumentKind_Invalid(9),
    ;
    
    companion object {
        fun byValue(value: Int) = CXTemplateArgumentKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXTemplateArgumentKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXTypeLayoutError(val value: Int) {
    CXTypeLayoutError_Invalid(-1),
    CXTypeLayoutError_Incomplete(-2),
    CXTypeLayoutError_Dependent(-3),
    CXTypeLayoutError_NotConstantSize(-4),
    CXTypeLayoutError_InvalidFieldName(-5),
    ;
    
    companion object {
        fun byValue(value: Int) = CXTypeLayoutError.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXTypeLayoutError
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXRefQualifierKind(val value: Int) {
    CXRefQualifier_None(0),
    CXRefQualifier_LValue(1),
    CXRefQualifier_RValue(2),
    ;
    
    companion object {
        fun byValue(value: Int) = CXRefQualifierKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXRefQualifierKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CX_CXXAccessSpecifier(val value: Int) {
    CX_CXXInvalidAccessSpecifier(0),
    CX_CXXPublic(1),
    CX_CXXProtected(2),
    CX_CXXPrivate(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CX_CXXAccessSpecifier.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CX_CXXAccessSpecifier
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CX_StorageClass(val value: Int) {
    CX_SC_Invalid(0),
    CX_SC_None(1),
    CX_SC_Extern(2),
    CX_SC_Static(3),
    CX_SC_PrivateExtern(4),
    CX_SC_OpenCLWorkGroupLocal(5),
    CX_SC_Auto(6),
    CX_SC_Register(7),
    ;
    
    companion object {
        fun byValue(value: Int) = CX_StorageClass.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CX_StorageClass
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXChildVisitResult(val value: Int) {
    CXChildVisit_Break(0),
    CXChildVisit_Continue(1),
    CXChildVisit_Recurse(2),
    ;
    
    companion object {
        fun byValue(value: Int) = CXChildVisitResult.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXChildVisitResult
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXObjCPropertyAttrKind(val value: Int) {
    CXObjCPropertyAttr_noattr(0),
    CXObjCPropertyAttr_readonly(1),
    CXObjCPropertyAttr_getter(2),
    CXObjCPropertyAttr_assign(4),
    CXObjCPropertyAttr_readwrite(8),
    CXObjCPropertyAttr_retain(16),
    CXObjCPropertyAttr_copy(32),
    CXObjCPropertyAttr_nonatomic(64),
    CXObjCPropertyAttr_setter(128),
    CXObjCPropertyAttr_atomic(256),
    CXObjCPropertyAttr_weak(512),
    CXObjCPropertyAttr_strong(1024),
    CXObjCPropertyAttr_unsafe_unretained(2048),
    ;
    
    companion object {
        fun byValue(value: Int) = CXObjCPropertyAttrKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXObjCPropertyAttrKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXObjCDeclQualifierKind(val value: Int) {
    CXObjCDeclQualifier_None(0),
    CXObjCDeclQualifier_In(1),
    CXObjCDeclQualifier_Inout(2),
    CXObjCDeclQualifier_Out(4),
    CXObjCDeclQualifier_Bycopy(8),
    CXObjCDeclQualifier_Byref(16),
    CXObjCDeclQualifier_Oneway(32),
    ;
    
    companion object {
        fun byValue(value: Int) = CXObjCDeclQualifierKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXObjCDeclQualifierKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXNameRefFlags(val value: Int) {
    CXNameRange_WantQualifier(1),
    CXNameRange_WantTemplateArgs(2),
    CXNameRange_WantSinglePiece(4),
    ;
    
    companion object {
        fun byValue(value: Int) = CXNameRefFlags.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXNameRefFlags
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXTokenKind(val value: Int) {
    CXToken_Punctuation(0),
    CXToken_Keyword(1),
    CXToken_Identifier(2),
    CXToken_Literal(3),
    CXToken_Comment(4),
    ;
    
    companion object {
        fun byValue(value: Int) = CXTokenKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXTokenKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXCompletionChunkKind(val value: Int) {
    CXCompletionChunk_Optional(0),
    CXCompletionChunk_TypedText(1),
    CXCompletionChunk_Text(2),
    CXCompletionChunk_Placeholder(3),
    CXCompletionChunk_Informative(4),
    CXCompletionChunk_CurrentParameter(5),
    CXCompletionChunk_LeftParen(6),
    CXCompletionChunk_RightParen(7),
    CXCompletionChunk_LeftBracket(8),
    CXCompletionChunk_RightBracket(9),
    CXCompletionChunk_LeftBrace(10),
    CXCompletionChunk_RightBrace(11),
    CXCompletionChunk_LeftAngle(12),
    CXCompletionChunk_RightAngle(13),
    CXCompletionChunk_Comma(14),
    CXCompletionChunk_ResultType(15),
    CXCompletionChunk_Colon(16),
    CXCompletionChunk_SemiColon(17),
    CXCompletionChunk_Equal(18),
    CXCompletionChunk_HorizontalSpace(19),
    CXCompletionChunk_VerticalSpace(20),
    ;
    
    companion object {
        fun byValue(value: Int) = CXCompletionChunkKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXCompletionChunkKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXCodeComplete_Flags(val value: Int) {
    CXCodeComplete_IncludeMacros(1),
    CXCodeComplete_IncludeCodePatterns(2),
    CXCodeComplete_IncludeBriefComments(4),
    ;
    
    companion object {
        fun byValue(value: Int) = CXCodeComplete_Flags.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXCodeComplete_Flags
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXCompletionContext(val value: Int) {
    CXCompletionContext_Unexposed(0),
    CXCompletionContext_AnyType(1),
    CXCompletionContext_AnyValue(2),
    CXCompletionContext_ObjCObjectValue(4),
    CXCompletionContext_ObjCSelectorValue(8),
    CXCompletionContext_CXXClassTypeValue(16),
    CXCompletionContext_DotMemberAccess(32),
    CXCompletionContext_ArrowMemberAccess(64),
    CXCompletionContext_ObjCPropertyAccess(128),
    CXCompletionContext_EnumTag(256),
    CXCompletionContext_UnionTag(512),
    CXCompletionContext_StructTag(1024),
    CXCompletionContext_ClassTag(2048),
    CXCompletionContext_Namespace(4096),
    CXCompletionContext_NestedNameSpecifier(8192),
    CXCompletionContext_ObjCInterface(16384),
    CXCompletionContext_ObjCProtocol(32768),
    CXCompletionContext_ObjCCategory(65536),
    CXCompletionContext_ObjCInstanceMessage(131072),
    CXCompletionContext_ObjCClassMessage(262144),
    CXCompletionContext_ObjCSelectorName(524288),
    CXCompletionContext_MacroName(1048576),
    CXCompletionContext_NaturalLanguage(2097152),
    CXCompletionContext_Unknown(4194303),
    ;
    
    companion object {
        fun byValue(value: Int) = CXCompletionContext.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXCompletionContext
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXVisitorResult(val value: Int) {
    CXVisit_Break(0),
    CXVisit_Continue(1),
    ;
    
    companion object {
        fun byValue(value: Int) = CXVisitorResult.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXVisitorResult
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXResult(val value: Int) {
    CXResult_Success(0),
    CXResult_Invalid(1),
    CXResult_VisitBreak(2),
    ;
    
    companion object {
        fun byValue(value: Int) = CXResult.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXResult
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXIdxEntityKind(val value: Int) {
    CXIdxEntity_Unexposed(0),
    CXIdxEntity_Typedef(1),
    CXIdxEntity_Function(2),
    CXIdxEntity_Variable(3),
    CXIdxEntity_Field(4),
    CXIdxEntity_EnumConstant(5),
    CXIdxEntity_ObjCClass(6),
    CXIdxEntity_ObjCProtocol(7),
    CXIdxEntity_ObjCCategory(8),
    CXIdxEntity_ObjCInstanceMethod(9),
    CXIdxEntity_ObjCClassMethod(10),
    CXIdxEntity_ObjCProperty(11),
    CXIdxEntity_ObjCIvar(12),
    CXIdxEntity_Enum(13),
    CXIdxEntity_Struct(14),
    CXIdxEntity_Union(15),
    CXIdxEntity_CXXClass(16),
    CXIdxEntity_CXXNamespace(17),
    CXIdxEntity_CXXNamespaceAlias(18),
    CXIdxEntity_CXXStaticVariable(19),
    CXIdxEntity_CXXStaticMethod(20),
    CXIdxEntity_CXXInstanceMethod(21),
    CXIdxEntity_CXXConstructor(22),
    CXIdxEntity_CXXDestructor(23),
    CXIdxEntity_CXXConversionFunction(24),
    CXIdxEntity_CXXTypeAlias(25),
    CXIdxEntity_CXXInterface(26),
    ;
    
    companion object {
        fun byValue(value: Int) = CXIdxEntityKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXIdxEntityKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXIdxEntityLanguage(val value: Int) {
    CXIdxEntityLang_None(0),
    CXIdxEntityLang_C(1),
    CXIdxEntityLang_ObjC(2),
    CXIdxEntityLang_CXX(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CXIdxEntityLanguage.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXIdxEntityLanguage
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXIdxEntityCXXTemplateKind(val value: Int) {
    CXIdxEntity_NonTemplate(0),
    CXIdxEntity_Template(1),
    CXIdxEntity_TemplatePartialSpecialization(2),
    CXIdxEntity_TemplateSpecialization(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CXIdxEntityCXXTemplateKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXIdxEntityCXXTemplateKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXIdxAttrKind(val value: Int) {
    CXIdxAttr_Unexposed(0),
    CXIdxAttr_IBAction(1),
    CXIdxAttr_IBOutlet(2),
    CXIdxAttr_IBOutletCollection(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CXIdxAttrKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXIdxAttrKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXIdxDeclInfoFlags(val value: Int) {
    CXIdxDeclFlag_Skipped(1),
    ;
    
    companion object {
        fun byValue(value: Int) = CXIdxDeclInfoFlags.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXIdxDeclInfoFlags
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXIdxObjCContainerKind(val value: Int) {
    CXIdxObjCContainer_ForwardRef(0),
    CXIdxObjCContainer_Interface(1),
    CXIdxObjCContainer_Implementation(2),
    ;
    
    companion object {
        fun byValue(value: Int) = CXIdxObjCContainerKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXIdxObjCContainerKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXIdxEntityRefKind(val value: Int) {
    CXIdxEntityRef_Direct(1),
    CXIdxEntityRef_Implicit(2),
    ;
    
    companion object {
        fun byValue(value: Int) = CXIdxEntityRefKind.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXIdxEntityRefKind
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

enum class CXIndexOptFlags(val value: Int) {
    CXIndexOpt_None(0),
    CXIndexOpt_SuppressRedundantRefs(1),
    CXIndexOpt_IndexFunctionLocalSymbols(2),
    CXIndexOpt_IndexImplicitTemplateInstantiations(4),
    CXIndexOpt_SuppressWarnings(8),
    CXIndexOpt_SkipParsedBodiesInSession(16),
    ;
    
    companion object {
        fun byValue(value: Int) = CXIndexOptFlags.values().find { it.value == value }!!
    }
    
    class ref(ptr: NativePtr) : NativeRef(ptr) {
        companion object : TypeWithSize<ref>(Int32Box.size, ::ref)
        var value: CXIndexOptFlags
            get() = byValue(Int32Box.byPtr(ptr).value)
            set(value) { Int32Box.byPtr(ptr).value = value.value }
    }
}

object externals {
    init { System.loadLibrary("clangbridge") }
    external fun asctime(arg0: Long): Long
    
    external fun clock(): Long
    
    external fun ctime(arg0: Long): Long
    
    external fun getdate(arg0: Long): Long
    
    external fun gmtime(arg0: Long): Long
    
    external fun localtime(arg0: Long): Long
    
    external fun mktime(arg0: Long): Long
    
    external fun strftime(arg0: Long, arg1: Long, arg2: Long, arg3: Long): Long
    
    external fun strptime(arg0: Long, arg1: Long, arg2: Long): Long
    
    external fun time(arg0: Long): Long
    
    external fun tzset(): Unit
    
    external fun asctime_r(arg0: Long, arg1: Long): Long
    
    external fun ctime_r(arg0: Long, arg1: Long): Long
    
    external fun gmtime_r(arg0: Long, arg1: Long): Long
    
    external fun localtime_r(arg0: Long, arg1: Long): Long
    
    external fun posix2time(arg0: Long): Long
    
    external fun tzsetwall(): Unit
    
    external fun time2posix(arg0: Long): Long
    
    external fun timelocal(arg0: Long): Long
    
    external fun timegm(arg0: Long): Long
    
    external fun nanosleep(arg0: Long, arg1: Long): Int
    
    external fun clang_getCString(string: Long): Long
    
    external fun clang_disposeString(string: Long): Unit
    
    external fun clang_disposeStringSet(set: Long): Unit
    
    external fun clang_getBuildSessionTimestamp(): Long
    
    external fun clang_VirtualFileOverlay_create(options: Int): Long
    
    external fun clang_VirtualFileOverlay_addFileMapping(arg0: Long, virtualPath: Long, realPath: Long): Int
    
    external fun clang_VirtualFileOverlay_setCaseSensitivity(arg0: Long, caseSensitive: Int): Int
    
    external fun clang_VirtualFileOverlay_writeToBuffer(arg0: Long, options: Int, out_buffer_ptr: Long, out_buffer_size: Long): Int
    
    external fun clang_free(buffer: Long): Unit
    
    external fun clang_VirtualFileOverlay_dispose(arg0: Long): Unit
    
    external fun clang_ModuleMapDescriptor_create(options: Int): Long
    
    external fun clang_ModuleMapDescriptor_setFrameworkModuleName(arg0: Long, name: Long): Int
    
    external fun clang_ModuleMapDescriptor_setUmbrellaHeader(arg0: Long, name: Long): Int
    
    external fun clang_ModuleMapDescriptor_writeToBuffer(arg0: Long, options: Int, out_buffer_ptr: Long, out_buffer_size: Long): Int
    
    external fun clang_ModuleMapDescriptor_dispose(arg0: Long): Unit
    
    external fun clang_createIndex(excludeDeclarationsFromPCH: Int, displayDiagnostics: Int): Long
    
    external fun clang_disposeIndex(index: Long): Unit
    
    external fun clang_CXIndex_setGlobalOptions(arg0: Long, options: Int): Unit
    
    external fun clang_CXIndex_getGlobalOptions(arg0: Long): Int
    
    external fun clang_getFileName(SFile: Long, retValPlacement: Long): Long
    
    external fun clang_getFileTime(SFile: Long): Long
    
    external fun clang_getFileUniqueID(file: Long, outID: Long): Int
    
    external fun clang_isFileMultipleIncludeGuarded(tu: Long, file: Long): Int
    
    external fun clang_getFile(tu: Long, file_name: Long): Long
    
    external fun clang_File_isEqual(file1: Long, file2: Long): Int
    
    external fun clang_getNullLocation(retValPlacement: Long): Long
    
    external fun clang_equalLocations(loc1: Long, loc2: Long): Int
    
    external fun clang_getLocation(tu: Long, file: Long, line: Int, column: Int, retValPlacement: Long): Long
    
    external fun clang_getLocationForOffset(tu: Long, file: Long, offset: Int, retValPlacement: Long): Long
    
    external fun clang_Location_isInSystemHeader(location: Long): Int
    
    external fun clang_Location_isFromMainFile(location: Long): Int
    
    external fun clang_getNullRange(retValPlacement: Long): Long
    
    external fun clang_getRange(begin: Long, end: Long, retValPlacement: Long): Long
    
    external fun clang_equalRanges(range1: Long, range2: Long): Int
    
    external fun clang_Range_isNull(range: Long): Int
    
    external fun clang_getExpansionLocation(location: Long, file: Long, line: Long, column: Long, offset: Long): Unit
    
    external fun clang_getPresumedLocation(location: Long, filename: Long, line: Long, column: Long): Unit
    
    external fun clang_getInstantiationLocation(location: Long, file: Long, line: Long, column: Long, offset: Long): Unit
    
    external fun clang_getSpellingLocation(location: Long, file: Long, line: Long, column: Long, offset: Long): Unit
    
    external fun clang_getFileLocation(location: Long, file: Long, line: Long, column: Long, offset: Long): Unit
    
    external fun clang_getRangeStart(range: Long, retValPlacement: Long): Long
    
    external fun clang_getRangeEnd(range: Long, retValPlacement: Long): Long
    
    external fun clang_getSkippedRanges(tu: Long, file: Long): Long
    
    external fun clang_disposeSourceRangeList(ranges: Long): Unit
    
    external fun clang_getNumDiagnosticsInSet(Diags: Long): Int
    
    external fun clang_getDiagnosticInSet(Diags: Long, Index: Int): Long
    
    external fun clang_loadDiagnostics(file: Long, error: Long, errorString: Long): Long
    
    external fun clang_disposeDiagnosticSet(Diags: Long): Unit
    
    external fun clang_getChildDiagnostics(D: Long): Long
    
    external fun clang_getNumDiagnostics(Unit: Long): Int
    
    external fun clang_getDiagnostic(Unit: Long, Index: Int): Long
    
    external fun clang_getDiagnosticSetFromTU(Unit: Long): Long
    
    external fun clang_disposeDiagnostic(Diagnostic: Long): Unit
    
    external fun clang_formatDiagnostic(Diagnostic: Long, Options: Int, retValPlacement: Long): Long
    
    external fun clang_defaultDiagnosticDisplayOptions(): Int
    
    external fun clang_getDiagnosticSeverity(arg0: Long): Int
    
    external fun clang_getDiagnosticLocation(arg0: Long, retValPlacement: Long): Long
    
    external fun clang_getDiagnosticSpelling(arg0: Long, retValPlacement: Long): Long
    
    external fun clang_getDiagnosticOption(Diag: Long, Disable: Long, retValPlacement: Long): Long
    
    external fun clang_getDiagnosticCategory(arg0: Long): Int
    
    external fun clang_getDiagnosticCategoryName(Category: Int, retValPlacement: Long): Long
    
    external fun clang_getDiagnosticCategoryText(arg0: Long, retValPlacement: Long): Long
    
    external fun clang_getDiagnosticNumRanges(arg0: Long): Int
    
    external fun clang_getDiagnosticRange(Diagnostic: Long, Range: Int, retValPlacement: Long): Long
    
    external fun clang_getDiagnosticNumFixIts(Diagnostic: Long): Int
    
    external fun clang_getDiagnosticFixIt(Diagnostic: Long, FixIt: Int, ReplacementRange: Long, retValPlacement: Long): Long
    
    external fun clang_getTranslationUnitSpelling(CTUnit: Long, retValPlacement: Long): Long
    
    external fun clang_createTranslationUnitFromSourceFile(CIdx: Long, source_filename: Long, num_clang_command_line_args: Int, clang_command_line_args: Long, num_unsaved_files: Int, unsaved_files: Long): Long
    
    external fun clang_createTranslationUnit(CIdx: Long, ast_filename: Long): Long
    
    external fun clang_createTranslationUnit2(CIdx: Long, ast_filename: Long, out_TU: Long): Int
    
    external fun clang_defaultEditingTranslationUnitOptions(): Int
    
    external fun clang_parseTranslationUnit(CIdx: Long, source_filename: Long, command_line_args: Long, num_command_line_args: Int, unsaved_files: Long, num_unsaved_files: Int, options: Int): Long
    
    external fun clang_parseTranslationUnit2(CIdx: Long, source_filename: Long, command_line_args: Long, num_command_line_args: Int, unsaved_files: Long, num_unsaved_files: Int, options: Int, out_TU: Long): Int
    
    external fun clang_parseTranslationUnit2FullArgv(CIdx: Long, source_filename: Long, command_line_args: Long, num_command_line_args: Int, unsaved_files: Long, num_unsaved_files: Int, options: Int, out_TU: Long): Int
    
    external fun clang_defaultSaveOptions(TU: Long): Int
    
    external fun clang_saveTranslationUnit(TU: Long, FileName: Long, options: Int): Int
    
    external fun clang_disposeTranslationUnit(arg0: Long): Unit
    
    external fun clang_defaultReparseOptions(TU: Long): Int
    
    external fun clang_reparseTranslationUnit(TU: Long, num_unsaved_files: Int, unsaved_files: Long, options: Int): Int
    
    external fun clang_getTUResourceUsageName(kind: Int): Long
    
    external fun clang_getCXTUResourceUsage(TU: Long, retValPlacement: Long): Long
    
    external fun clang_disposeCXTUResourceUsage(usage: Long): Unit
    
    external fun clang_getNullCursor(retValPlacement: Long): Long
    
    external fun clang_getTranslationUnitCursor(arg0: Long, retValPlacement: Long): Long
    
    external fun clang_equalCursors(arg0: Long, arg1: Long): Int
    
    external fun clang_Cursor_isNull(cursor: Long): Int
    
    external fun clang_hashCursor(arg0: Long): Int
    
    external fun clang_getCursorKind(arg0: Long): Int
    
    external fun clang_isDeclaration(arg0: Int): Int
    
    external fun clang_isReference(arg0: Int): Int
    
    external fun clang_isExpression(arg0: Int): Int
    
    external fun clang_isStatement(arg0: Int): Int
    
    external fun clang_isAttribute(arg0: Int): Int
    
    external fun clang_isInvalid(arg0: Int): Int
    
    external fun clang_isTranslationUnit(arg0: Int): Int
    
    external fun clang_isPreprocessing(arg0: Int): Int
    
    external fun clang_isUnexposed(arg0: Int): Int
    
    external fun clang_getCursorLinkage(cursor: Long): Int
    
    external fun clang_getCursorVisibility(cursor: Long): Int
    
    external fun clang_getCursorAvailability(cursor: Long): Int
    
    external fun clang_getCursorPlatformAvailability(cursor: Long, always_deprecated: Long, deprecated_message: Long, always_unavailable: Long, unavailable_message: Long, availability: Long, availability_size: Int): Int
    
    external fun clang_disposeCXPlatformAvailability(availability: Long): Unit
    
    external fun clang_getCursorLanguage(cursor: Long): Int
    
    external fun clang_Cursor_getTranslationUnit(arg0: Long): Long
    
    external fun clang_createCXCursorSet(): Long
    
    external fun clang_disposeCXCursorSet(cset: Long): Unit
    
    external fun clang_CXCursorSet_contains(cset: Long, cursor: Long): Int
    
    external fun clang_CXCursorSet_insert(cset: Long, cursor: Long): Int
    
    external fun clang_getCursorSemanticParent(cursor: Long, retValPlacement: Long): Long
    
    external fun clang_getCursorLexicalParent(cursor: Long, retValPlacement: Long): Long
    
    external fun clang_getOverriddenCursors(cursor: Long, overridden: Long, num_overridden: Long): Unit
    
    external fun clang_disposeOverriddenCursors(overridden: Long): Unit
    
    external fun clang_getIncludedFile(cursor: Long): Long
    
    external fun clang_getCursor(arg0: Long, arg1: Long, retValPlacement: Long): Long
    
    external fun clang_getCursorLocation(arg0: Long, retValPlacement: Long): Long
    
    external fun clang_getCursorExtent(arg0: Long, retValPlacement: Long): Long
    
    external fun clang_getCursorType(C: Long, retValPlacement: Long): Long
    
    external fun clang_getTypeSpelling(CT: Long, retValPlacement: Long): Long
    
    external fun clang_getTypedefDeclUnderlyingType(C: Long, retValPlacement: Long): Long
    
    external fun clang_getEnumDeclIntegerType(C: Long, retValPlacement: Long): Long
    
    external fun clang_getEnumConstantDeclValue(C: Long): Long
    
    external fun clang_getEnumConstantDeclUnsignedValue(C: Long): Long
    
    external fun clang_getFieldDeclBitWidth(C: Long): Int
    
    external fun clang_Cursor_getNumArguments(C: Long): Int
    
    external fun clang_Cursor_getArgument(C: Long, i: Int, retValPlacement: Long): Long
    
    external fun clang_Cursor_getNumTemplateArguments(C: Long): Int
    
    external fun clang_Cursor_getTemplateArgumentKind(C: Long, I: Int): Int
    
    external fun clang_Cursor_getTemplateArgumentType(C: Long, I: Int, retValPlacement: Long): Long
    
    external fun clang_Cursor_getTemplateArgumentValue(C: Long, I: Int): Long
    
    external fun clang_Cursor_getTemplateArgumentUnsignedValue(C: Long, I: Int): Long
    
    external fun clang_equalTypes(A: Long, B: Long): Int
    
    external fun clang_getCanonicalType(T: Long, retValPlacement: Long): Long
    
    external fun clang_isConstQualifiedType(T: Long): Int
    
    external fun clang_isVolatileQualifiedType(T: Long): Int
    
    external fun clang_isRestrictQualifiedType(T: Long): Int
    
    external fun clang_getPointeeType(T: Long, retValPlacement: Long): Long
    
    external fun clang_getTypeDeclaration(T: Long, retValPlacement: Long): Long
    
    external fun clang_getDeclObjCTypeEncoding(C: Long, retValPlacement: Long): Long
    
    external fun clang_getTypeKindSpelling(K: Int, retValPlacement: Long): Long
    
    external fun clang_getFunctionTypeCallingConv(T: Long): Int
    
    external fun clang_getResultType(T: Long, retValPlacement: Long): Long
    
    external fun clang_getNumArgTypes(T: Long): Int
    
    external fun clang_getArgType(T: Long, i: Int, retValPlacement: Long): Long
    
    external fun clang_isFunctionTypeVariadic(T: Long): Int
    
    external fun clang_getCursorResultType(C: Long, retValPlacement: Long): Long
    
    external fun clang_isPODType(T: Long): Int
    
    external fun clang_getElementType(T: Long, retValPlacement: Long): Long
    
    external fun clang_getNumElements(T: Long): Long
    
    external fun clang_getArrayElementType(T: Long, retValPlacement: Long): Long
    
    external fun clang_getArraySize(T: Long): Long
    
    external fun clang_Type_getAlignOf(T: Long): Long
    
    external fun clang_Type_getClassType(T: Long, retValPlacement: Long): Long
    
    external fun clang_Type_getSizeOf(T: Long): Long
    
    external fun clang_Type_getOffsetOf(T: Long, S: Long): Long
    
    external fun clang_Cursor_getOffsetOfField(C: Long): Long
    
    external fun clang_Cursor_isAnonymous(C: Long): Int
    
    external fun clang_Type_getNumTemplateArguments(T: Long): Int
    
    external fun clang_Type_getTemplateArgumentAsType(T: Long, i: Int, retValPlacement: Long): Long
    
    external fun clang_Type_getCXXRefQualifier(T: Long): Int
    
    external fun clang_Cursor_isBitField(C: Long): Int
    
    external fun clang_isVirtualBase(arg0: Long): Int
    
    external fun clang_getCXXAccessSpecifier(arg0: Long): Int
    
    external fun clang_Cursor_getStorageClass(arg0: Long): Int
    
    external fun clang_getNumOverloadedDecls(cursor: Long): Int
    
    external fun clang_getOverloadedDecl(cursor: Long, index: Int, retValPlacement: Long): Long
    
    external fun clang_getIBOutletCollectionType(arg0: Long, retValPlacement: Long): Long
    
    external fun clang_visitChildren(parent: Long, visitor: Long, client_data: Long): Int
    
    external fun clang_getCursorUSR(arg0: Long, retValPlacement: Long): Long
    
    external fun clang_constructUSR_ObjCClass(class_name: Long, retValPlacement: Long): Long
    
    external fun clang_constructUSR_ObjCCategory(class_name: Long, category_name: Long, retValPlacement: Long): Long
    
    external fun clang_constructUSR_ObjCProtocol(protocol_name: Long, retValPlacement: Long): Long
    
    external fun clang_constructUSR_ObjCIvar(name: Long, classUSR: Long, retValPlacement: Long): Long
    
    external fun clang_constructUSR_ObjCMethod(name: Long, isInstanceMethod: Int, classUSR: Long, retValPlacement: Long): Long
    
    external fun clang_constructUSR_ObjCProperty(property: Long, classUSR: Long, retValPlacement: Long): Long
    
    external fun clang_getCursorSpelling(arg0: Long, retValPlacement: Long): Long
    
    external fun clang_Cursor_getSpellingNameRange(arg0: Long, pieceIndex: Int, options: Int, retValPlacement: Long): Long
    
    external fun clang_getCursorDisplayName(arg0: Long, retValPlacement: Long): Long
    
    external fun clang_getCursorReferenced(arg0: Long, retValPlacement: Long): Long
    
    external fun clang_getCursorDefinition(arg0: Long, retValPlacement: Long): Long
    
    external fun clang_isCursorDefinition(arg0: Long): Int
    
    external fun clang_getCanonicalCursor(arg0: Long, retValPlacement: Long): Long
    
    external fun clang_Cursor_getObjCSelectorIndex(arg0: Long): Int
    
    external fun clang_Cursor_isDynamicCall(C: Long): Int
    
    external fun clang_Cursor_getReceiverType(C: Long, retValPlacement: Long): Long
    
    external fun clang_Cursor_getObjCPropertyAttributes(C: Long, reserved: Int): Int
    
    external fun clang_Cursor_getObjCDeclQualifiers(C: Long): Int
    
    external fun clang_Cursor_isObjCOptional(C: Long): Int
    
    external fun clang_Cursor_isVariadic(C: Long): Int
    
    external fun clang_Cursor_getCommentRange(C: Long, retValPlacement: Long): Long
    
    external fun clang_Cursor_getRawCommentText(C: Long, retValPlacement: Long): Long
    
    external fun clang_Cursor_getBriefCommentText(C: Long, retValPlacement: Long): Long
    
    external fun clang_Cursor_getMangling(arg0: Long, retValPlacement: Long): Long
    
    external fun clang_Cursor_getCXXManglings(arg0: Long): Long
    
    external fun clang_Cursor_getModule(C: Long): Long
    
    external fun clang_getModuleForFile(arg0: Long, arg1: Long): Long
    
    external fun clang_Module_getASTFile(Module: Long): Long
    
    external fun clang_Module_getParent(Module: Long): Long
    
    external fun clang_Module_getName(Module: Long, retValPlacement: Long): Long
    
    external fun clang_Module_getFullName(Module: Long, retValPlacement: Long): Long
    
    external fun clang_Module_isSystem(Module: Long): Int
    
    external fun clang_Module_getNumTopLevelHeaders(arg0: Long, Module: Long): Int
    
    external fun clang_Module_getTopLevelHeader(arg0: Long, Module: Long, Index: Int): Long
    
    external fun clang_CXXField_isMutable(C: Long): Int
    
    external fun clang_CXXMethod_isPureVirtual(C: Long): Int
    
    external fun clang_CXXMethod_isStatic(C: Long): Int
    
    external fun clang_CXXMethod_isVirtual(C: Long): Int
    
    external fun clang_CXXMethod_isConst(C: Long): Int
    
    external fun clang_getTemplateCursorKind(C: Long): Int
    
    external fun clang_getSpecializedCursorTemplate(C: Long, retValPlacement: Long): Long
    
    external fun clang_getCursorReferenceNameRange(C: Long, NameFlags: Int, PieceIndex: Int, retValPlacement: Long): Long
    
    external fun clang_getTokenKind(arg0: Long): Int
    
    external fun clang_getTokenSpelling(arg0: Long, arg1: Long, retValPlacement: Long): Long
    
    external fun clang_getTokenLocation(arg0: Long, arg1: Long, retValPlacement: Long): Long
    
    external fun clang_getTokenExtent(arg0: Long, arg1: Long, retValPlacement: Long): Long
    
    external fun clang_tokenize(TU: Long, Range: Long, Tokens: Long, NumTokens: Long): Unit
    
    external fun clang_annotateTokens(TU: Long, Tokens: Long, NumTokens: Int, Cursors: Long): Unit
    
    external fun clang_disposeTokens(TU: Long, Tokens: Long, NumTokens: Int): Unit
    
    external fun clang_getCursorKindSpelling(Kind: Int, retValPlacement: Long): Long
    
    external fun clang_getDefinitionSpellingAndExtent(arg0: Long, startBuf: Long, endBuf: Long, startLine: Long, startColumn: Long, endLine: Long, endColumn: Long): Unit
    
    external fun clang_enableStackTraces(): Unit
    
    external fun clang_executeOnThread(fn: Long, user_data: Long, stack_size: Int): Unit
    
    external fun clang_getCompletionChunkKind(completion_string: Long, chunk_number: Int): Int
    
    external fun clang_getCompletionChunkText(completion_string: Long, chunk_number: Int, retValPlacement: Long): Long
    
    external fun clang_getCompletionChunkCompletionString(completion_string: Long, chunk_number: Int): Long
    
    external fun clang_getNumCompletionChunks(completion_string: Long): Int
    
    external fun clang_getCompletionPriority(completion_string: Long): Int
    
    external fun clang_getCompletionAvailability(completion_string: Long): Int
    
    external fun clang_getCompletionNumAnnotations(completion_string: Long): Int
    
    external fun clang_getCompletionAnnotation(completion_string: Long, annotation_number: Int, retValPlacement: Long): Long
    
    external fun clang_getCompletionParent(completion_string: Long, kind: Long, retValPlacement: Long): Long
    
    external fun clang_getCompletionBriefComment(completion_string: Long, retValPlacement: Long): Long
    
    external fun clang_getCursorCompletionString(cursor: Long): Long
    
    external fun clang_defaultCodeCompleteOptions(): Int
    
    external fun clang_codeCompleteAt(TU: Long, complete_filename: Long, complete_line: Int, complete_column: Int, unsaved_files: Long, num_unsaved_files: Int, options: Int): Long
    
    external fun clang_sortCodeCompletionResults(Results: Long, NumResults: Int): Unit
    
    external fun clang_disposeCodeCompleteResults(Results: Long): Unit
    
    external fun clang_codeCompleteGetNumDiagnostics(Results: Long): Int
    
    external fun clang_codeCompleteGetDiagnostic(Results: Long, Index: Int): Long
    
    external fun clang_codeCompleteGetContexts(Results: Long): Long
    
    external fun clang_codeCompleteGetContainerKind(Results: Long, IsIncomplete: Long): Int
    
    external fun clang_codeCompleteGetContainerUSR(Results: Long, retValPlacement: Long): Long
    
    external fun clang_codeCompleteGetObjCSelector(Results: Long, retValPlacement: Long): Long
    
    external fun clang_getClangVersion(retValPlacement: Long): Long
    
    external fun clang_toggleCrashRecovery(isEnabled: Int): Unit
    
    external fun clang_getInclusions(tu: Long, visitor: Long, client_data: Long): Unit
    
    external fun clang_getRemappings(path: Long): Long
    
    external fun clang_getRemappingsFromFileList(filePaths: Long, numFiles: Int): Long
    
    external fun clang_remap_getNumFiles(arg0: Long): Int
    
    external fun clang_remap_getFilenames(arg0: Long, index: Int, original: Long, transformed: Long): Unit
    
    external fun clang_remap_dispose(arg0: Long): Unit
    
    external fun clang_findReferencesInFile(cursor: Long, file: Long, visitor: Long): Int
    
    external fun clang_findIncludesInFile(TU: Long, file: Long, visitor: Long): Int
    
    external fun clang_index_isEntityObjCContainerKind(arg0: Int): Int
    
    external fun clang_index_getObjCContainerDeclInfo(arg0: Long): Long
    
    external fun clang_index_getObjCInterfaceDeclInfo(arg0: Long): Long
    
    external fun clang_index_getObjCCategoryDeclInfo(arg0: Long): Long
    
    external fun clang_index_getObjCProtocolRefListInfo(arg0: Long): Long
    
    external fun clang_index_getObjCPropertyDeclInfo(arg0: Long): Long
    
    external fun clang_index_getIBOutletCollectionAttrInfo(arg0: Long): Long
    
    external fun clang_index_getCXXClassDeclInfo(arg0: Long): Long
    
    external fun clang_index_getClientContainer(arg0: Long): Long
    
    external fun clang_index_setClientContainer(arg0: Long, arg1: Long): Unit
    
    external fun clang_index_getClientEntity(arg0: Long): Long
    
    external fun clang_index_setClientEntity(arg0: Long, arg1: Long): Unit
    
    external fun clang_IndexAction_create(CIdx: Long): Long
    
    external fun clang_IndexAction_dispose(arg0: Long): Unit
    
    external fun clang_indexSourceFile(arg0: Long, client_data: Long, index_callbacks: Long, index_callbacks_size: Int, index_options: Int, source_filename: Long, command_line_args: Long, num_command_line_args: Int, unsaved_files: Long, num_unsaved_files: Int, out_TU: Long, TU_options: Int): Int
    
    external fun clang_indexSourceFileFullArgv(arg0: Long, client_data: Long, index_callbacks: Long, index_callbacks_size: Int, index_options: Int, source_filename: Long, command_line_args: Long, num_command_line_args: Int, unsaved_files: Long, num_unsaved_files: Int, out_TU: Long, TU_options: Int): Int
    
    external fun clang_indexTranslationUnit(arg0: Long, client_data: Long, index_callbacks: Long, index_callbacks_size: Int, index_options: Int, arg5: Long): Int
    
    external fun clang_indexLoc_getFileLocation(loc: Long, indexFile: Long, file: Long, line: Long, column: Long, offset: Long): Unit
    
    external fun clang_indexLoc_getCXSourceLocation(loc: Long, retValPlacement: Long): Long
    
    external fun clang_Type_visitFields(T: Long, visitor: Long, client_data: Long): Int
    
}
