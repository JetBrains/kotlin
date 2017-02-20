package clang

import kotlinx.cinterop.*

fun asctime(arg0: CPointer<tm>?): CPointer<CInt8Var>? {
    val _arg0 = arg0.rawValue
    val res = externals.asctime(_arg0)
    return CPointer.createNullable<CInt8Var>(res)
}

fun clock(): clock_t {
    val res = externals.clock()
    return res
}

fun ctime(arg0: CPointer<time_tVar>?): CPointer<CInt8Var>? {
    val _arg0 = arg0.rawValue
    val res = externals.ctime(_arg0)
    return CPointer.createNullable<CInt8Var>(res)
}

fun difftime(arg0: time_t, arg1: time_t): Double {
    val _arg0 = arg0
    val _arg1 = arg1
    val res = externals.difftime(_arg0, _arg1)
    return res
}

fun getdate(arg0: String?): CPointer<tm>? {
    return memScoped {
        val _arg0 = arg0?.toCString(memScope).rawPtr
        val res = externals.getdate(_arg0)
        CPointer.createNullable<tm>(res)
    }
}

fun gmtime(arg0: CPointer<time_tVar>?): CPointer<tm>? {
    val _arg0 = arg0.rawValue
    val res = externals.gmtime(_arg0)
    return CPointer.createNullable<tm>(res)
}

fun localtime(arg0: CPointer<time_tVar>?): CPointer<tm>? {
    val _arg0 = arg0.rawValue
    val res = externals.localtime(_arg0)
    return CPointer.createNullable<tm>(res)
}

fun mktime(arg0: CPointer<tm>?): time_t {
    val _arg0 = arg0.rawValue
    val res = externals.mktime(_arg0)
    return res
}

fun strftime(arg0: String?, arg1: size_t, arg2: String?, arg3: CPointer<tm>?): size_t {
    return memScoped {
        val _arg0 = arg0?.toCString(memScope).rawPtr
        val _arg1 = arg1
        val _arg2 = arg2?.toCString(memScope).rawPtr
        val _arg3 = arg3.rawValue
        val res = externals.strftime(_arg0, _arg1, _arg2, _arg3)
        res
    }
}

fun strptime(arg0: String?, arg1: String?, arg2: CPointer<tm>?): CPointer<CInt8Var>? {
    return memScoped {
        val _arg0 = arg0?.toCString(memScope).rawPtr
        val _arg1 = arg1?.toCString(memScope).rawPtr
        val _arg2 = arg2.rawValue
        val res = externals.strptime(_arg0, _arg1, _arg2)
        CPointer.createNullable<CInt8Var>(res)
    }
}

fun time(arg0: CPointer<time_tVar>?): time_t {
    val _arg0 = arg0.rawValue
    val res = externals.time(_arg0)
    return res
}

fun tzset(): Unit {
    val res = externals.tzset()
    return res
}

fun asctime_r(arg0: CPointer<tm>?, arg1: String?): CPointer<CInt8Var>? {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _arg1 = arg1?.toCString(memScope).rawPtr
        val res = externals.asctime_r(_arg0, _arg1)
        CPointer.createNullable<CInt8Var>(res)
    }
}

fun ctime_r(arg0: CPointer<time_tVar>?, arg1: String?): CPointer<CInt8Var>? {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _arg1 = arg1?.toCString(memScope).rawPtr
        val res = externals.ctime_r(_arg0, _arg1)
        CPointer.createNullable<CInt8Var>(res)
    }
}

fun gmtime_r(arg0: CPointer<time_tVar>?, arg1: CPointer<tm>?): CPointer<tm>? {
    val _arg0 = arg0.rawValue
    val _arg1 = arg1.rawValue
    val res = externals.gmtime_r(_arg0, _arg1)
    return CPointer.createNullable<tm>(res)
}

fun localtime_r(arg0: CPointer<time_tVar>?, arg1: CPointer<tm>?): CPointer<tm>? {
    val _arg0 = arg0.rawValue
    val _arg1 = arg1.rawValue
    val res = externals.localtime_r(_arg0, _arg1)
    return CPointer.createNullable<tm>(res)
}

fun posix2time(arg0: time_t): time_t {
    val _arg0 = arg0
    val res = externals.posix2time(_arg0)
    return res
}

fun tzsetwall(): Unit {
    val res = externals.tzsetwall()
    return res
}

fun time2posix(arg0: time_t): time_t {
    val _arg0 = arg0
    val res = externals.time2posix(_arg0)
    return res
}

fun timelocal(arg0: CPointer<tm>?): time_t {
    val _arg0 = arg0.rawValue
    val res = externals.timelocal(_arg0)
    return res
}

fun timegm(arg0: CPointer<tm>?): time_t {
    val _arg0 = arg0.rawValue
    val res = externals.timegm(_arg0)
    return res
}

fun nanosleep(__rqtp: CPointer<timespec>?, __rmtp: CPointer<timespec>?): Int {
    val ___rqtp = __rqtp.rawValue
    val ___rmtp = __rmtp.rawValue
    val res = externals.nanosleep(___rqtp, ___rmtp)
    return res
}

fun clock_getres(__clock_id: clockid_t, __res: CPointer<timespec>?): Int {
    val ___clock_id = __clock_id.value
    val ___res = __res.rawValue
    val res = externals.clock_getres(___clock_id, ___res)
    return res
}

fun clock_gettime(__clock_id: clockid_t, __tp: CPointer<timespec>?): Int {
    val ___clock_id = __clock_id.value
    val ___tp = __tp.rawValue
    val res = externals.clock_gettime(___clock_id, ___tp)
    return res
}

fun clock_gettime_nsec_np(__clock_id: clockid_t): __uint64_t {
    val ___clock_id = __clock_id.value
    val res = externals.clock_gettime_nsec_np(___clock_id)
    return res
}

fun clock_settime(__clock_id: clockid_t, __tp: CPointer<timespec>?): Int {
    val ___clock_id = __clock_id.value
    val ___tp = __tp.rawValue
    val res = externals.clock_settime(___clock_id, ___tp)
    return res
}

fun clang_getCString(string: CXString): CPointer<CInt8Var>? {
    val _string = string.rawPtr
    val res = externals.clang_getCString(_string)
    return CPointer.createNullable<CInt8Var>(res)
}

fun clang_disposeString(string: CXString): Unit {
    val _string = string.rawPtr
    val res = externals.clang_disposeString(_string)
    return res
}

fun clang_disposeStringSet(set: CPointer<CXStringSet>?): Unit {
    val _set = set.rawValue
    val res = externals.clang_disposeStringSet(_set)
    return res
}

fun clang_getBuildSessionTimestamp(): Long {
    val res = externals.clang_getBuildSessionTimestamp()
    return res
}

fun clang_VirtualFileOverlay_create(options: Int): CXVirtualFileOverlay? {
    val _options = options
    val res = externals.clang_VirtualFileOverlay_create(_options)
    return CPointer.createNullable<CXVirtualFileOverlayImpl>(res)
}

fun clang_VirtualFileOverlay_addFileMapping(arg0: CXVirtualFileOverlay?, virtualPath: String?, realPath: String?): CXErrorCode {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _virtualPath = virtualPath?.toCString(memScope).rawPtr
        val _realPath = realPath?.toCString(memScope).rawPtr
        val res = externals.clang_VirtualFileOverlay_addFileMapping(_arg0, _virtualPath, _realPath)
        CXErrorCode.byValue(res)
    }
}

fun clang_VirtualFileOverlay_setCaseSensitivity(arg0: CXVirtualFileOverlay?, caseSensitive: Int): CXErrorCode {
    val _arg0 = arg0.rawValue
    val _caseSensitive = caseSensitive
    val res = externals.clang_VirtualFileOverlay_setCaseSensitivity(_arg0, _caseSensitive)
    return CXErrorCode.byValue(res)
}

fun clang_VirtualFileOverlay_writeToBuffer(arg0: CXVirtualFileOverlay?, options: Int, out_buffer_ptr: CPointer<CPointerVar<CInt8Var>>?, out_buffer_size: CPointer<CInt32Var>?): CXErrorCode {
    val _arg0 = arg0.rawValue
    val _options = options
    val _out_buffer_ptr = out_buffer_ptr.rawValue
    val _out_buffer_size = out_buffer_size.rawValue
    val res = externals.clang_VirtualFileOverlay_writeToBuffer(_arg0, _options, _out_buffer_ptr, _out_buffer_size)
    return CXErrorCode.byValue(res)
}

fun clang_free(buffer: COpaquePointer?): Unit {
    val _buffer = buffer.rawValue
    val res = externals.clang_free(_buffer)
    return res
}

fun clang_VirtualFileOverlay_dispose(arg0: CXVirtualFileOverlay?): Unit {
    val _arg0 = arg0.rawValue
    val res = externals.clang_VirtualFileOverlay_dispose(_arg0)
    return res
}

fun clang_ModuleMapDescriptor_create(options: Int): CXModuleMapDescriptor? {
    val _options = options
    val res = externals.clang_ModuleMapDescriptor_create(_options)
    return CPointer.createNullable<CXModuleMapDescriptorImpl>(res)
}

fun clang_ModuleMapDescriptor_setFrameworkModuleName(arg0: CXModuleMapDescriptor?, name: String?): CXErrorCode {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _name = name?.toCString(memScope).rawPtr
        val res = externals.clang_ModuleMapDescriptor_setFrameworkModuleName(_arg0, _name)
        CXErrorCode.byValue(res)
    }
}

fun clang_ModuleMapDescriptor_setUmbrellaHeader(arg0: CXModuleMapDescriptor?, name: String?): CXErrorCode {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _name = name?.toCString(memScope).rawPtr
        val res = externals.clang_ModuleMapDescriptor_setUmbrellaHeader(_arg0, _name)
        CXErrorCode.byValue(res)
    }
}

fun clang_ModuleMapDescriptor_writeToBuffer(arg0: CXModuleMapDescriptor?, options: Int, out_buffer_ptr: CPointer<CPointerVar<CInt8Var>>?, out_buffer_size: CPointer<CInt32Var>?): CXErrorCode {
    val _arg0 = arg0.rawValue
    val _options = options
    val _out_buffer_ptr = out_buffer_ptr.rawValue
    val _out_buffer_size = out_buffer_size.rawValue
    val res = externals.clang_ModuleMapDescriptor_writeToBuffer(_arg0, _options, _out_buffer_ptr, _out_buffer_size)
    return CXErrorCode.byValue(res)
}

fun clang_ModuleMapDescriptor_dispose(arg0: CXModuleMapDescriptor?): Unit {
    val _arg0 = arg0.rawValue
    val res = externals.clang_ModuleMapDescriptor_dispose(_arg0)
    return res
}

fun clang_createIndex(excludeDeclarationsFromPCH: Int, displayDiagnostics: Int): CXIndex? {
    val _excludeDeclarationsFromPCH = excludeDeclarationsFromPCH
    val _displayDiagnostics = displayDiagnostics
    val res = externals.clang_createIndex(_excludeDeclarationsFromPCH, _displayDiagnostics)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_disposeIndex(index: CXIndex?): Unit {
    val _index = index.rawValue
    val res = externals.clang_disposeIndex(_index)
    return res
}

fun clang_CXIndex_setGlobalOptions(arg0: CXIndex?, options: Int): Unit {
    val _arg0 = arg0.rawValue
    val _options = options
    val res = externals.clang_CXIndex_setGlobalOptions(_arg0, _options)
    return res
}

fun clang_CXIndex_getGlobalOptions(arg0: CXIndex?): Int {
    val _arg0 = arg0.rawValue
    val res = externals.clang_CXIndex_getGlobalOptions(_arg0)
    return res
}

fun clang_getFileName(SFile: CXFile?, retValPlacement: NativePlacement): CXString {
    val _SFile = SFile.rawValue
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getFileName(_SFile, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getFileTime(SFile: CXFile?): time_t {
    val _SFile = SFile.rawValue
    val res = externals.clang_getFileTime(_SFile)
    return res
}

fun clang_getFileUniqueID(file: CXFile?, outID: CPointer<CXFileUniqueID>?): Int {
    val _file = file.rawValue
    val _outID = outID.rawValue
    val res = externals.clang_getFileUniqueID(_file, _outID)
    return res
}

fun clang_isFileMultipleIncludeGuarded(tu: CXTranslationUnit?, file: CXFile?): Int {
    val _tu = tu.rawValue
    val _file = file.rawValue
    val res = externals.clang_isFileMultipleIncludeGuarded(_tu, _file)
    return res
}

fun clang_getFile(tu: CXTranslationUnit?, file_name: String?): CXFile? {
    return memScoped {
        val _tu = tu.rawValue
        val _file_name = file_name?.toCString(memScope).rawPtr
        val res = externals.clang_getFile(_tu, _file_name)
        CPointer.createNullable<COpaque>(res)
    }
}

fun clang_File_isEqual(file1: CXFile?, file2: CXFile?): Int {
    val _file1 = file1.rawValue
    val _file2 = file2.rawValue
    val res = externals.clang_File_isEqual(_file1, _file2)
    return res
}

fun clang_getNullLocation(retValPlacement: NativePlacement): CXSourceLocation {
    val _retValPlacement = retValPlacement.alloc<CXSourceLocation>().rawPtr
    val res = externals.clang_getNullLocation(_retValPlacement)
    return interpretPointed<CXSourceLocation>(res)
}

fun clang_equalLocations(loc1: CXSourceLocation, loc2: CXSourceLocation): Int {
    val _loc1 = loc1.rawPtr
    val _loc2 = loc2.rawPtr
    val res = externals.clang_equalLocations(_loc1, _loc2)
    return res
}

fun clang_getLocation(tu: CXTranslationUnit?, file: CXFile?, line: Int, column: Int, retValPlacement: NativePlacement): CXSourceLocation {
    val _tu = tu.rawValue
    val _file = file.rawValue
    val _line = line
    val _column = column
    val _retValPlacement = retValPlacement.alloc<CXSourceLocation>().rawPtr
    val res = externals.clang_getLocation(_tu, _file, _line, _column, _retValPlacement)
    return interpretPointed<CXSourceLocation>(res)
}

fun clang_getLocationForOffset(tu: CXTranslationUnit?, file: CXFile?, offset: Int, retValPlacement: NativePlacement): CXSourceLocation {
    val _tu = tu.rawValue
    val _file = file.rawValue
    val _offset = offset
    val _retValPlacement = retValPlacement.alloc<CXSourceLocation>().rawPtr
    val res = externals.clang_getLocationForOffset(_tu, _file, _offset, _retValPlacement)
    return interpretPointed<CXSourceLocation>(res)
}

fun clang_Location_isInSystemHeader(location: CXSourceLocation): Int {
    val _location = location.rawPtr
    val res = externals.clang_Location_isInSystemHeader(_location)
    return res
}

fun clang_Location_isFromMainFile(location: CXSourceLocation): Int {
    val _location = location.rawPtr
    val res = externals.clang_Location_isFromMainFile(_location)
    return res
}

fun clang_getNullRange(retValPlacement: NativePlacement): CXSourceRange {
    val _retValPlacement = retValPlacement.alloc<CXSourceRange>().rawPtr
    val res = externals.clang_getNullRange(_retValPlacement)
    return interpretPointed<CXSourceRange>(res)
}

fun clang_getRange(begin: CXSourceLocation, end: CXSourceLocation, retValPlacement: NativePlacement): CXSourceRange {
    val _begin = begin.rawPtr
    val _end = end.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXSourceRange>().rawPtr
    val res = externals.clang_getRange(_begin, _end, _retValPlacement)
    return interpretPointed<CXSourceRange>(res)
}

fun clang_equalRanges(range1: CXSourceRange, range2: CXSourceRange): Int {
    val _range1 = range1.rawPtr
    val _range2 = range2.rawPtr
    val res = externals.clang_equalRanges(_range1, _range2)
    return res
}

fun clang_Range_isNull(range: CXSourceRange): Int {
    val _range = range.rawPtr
    val res = externals.clang_Range_isNull(_range)
    return res
}

fun clang_getExpansionLocation(location: CXSourceLocation, file: CPointer<CXFileVar>?, line: CPointer<CInt32Var>?, column: CPointer<CInt32Var>?, offset: CPointer<CInt32Var>?): Unit {
    val _location = location.rawPtr
    val _file = file.rawValue
    val _line = line.rawValue
    val _column = column.rawValue
    val _offset = offset.rawValue
    val res = externals.clang_getExpansionLocation(_location, _file, _line, _column, _offset)
    return res
}

fun clang_getPresumedLocation(location: CXSourceLocation, filename: CPointer<CXString>?, line: CPointer<CInt32Var>?, column: CPointer<CInt32Var>?): Unit {
    val _location = location.rawPtr
    val _filename = filename.rawValue
    val _line = line.rawValue
    val _column = column.rawValue
    val res = externals.clang_getPresumedLocation(_location, _filename, _line, _column)
    return res
}

fun clang_getInstantiationLocation(location: CXSourceLocation, file: CPointer<CXFileVar>?, line: CPointer<CInt32Var>?, column: CPointer<CInt32Var>?, offset: CPointer<CInt32Var>?): Unit {
    val _location = location.rawPtr
    val _file = file.rawValue
    val _line = line.rawValue
    val _column = column.rawValue
    val _offset = offset.rawValue
    val res = externals.clang_getInstantiationLocation(_location, _file, _line, _column, _offset)
    return res
}

fun clang_getSpellingLocation(location: CXSourceLocation, file: CPointer<CXFileVar>?, line: CPointer<CInt32Var>?, column: CPointer<CInt32Var>?, offset: CPointer<CInt32Var>?): Unit {
    val _location = location.rawPtr
    val _file = file.rawValue
    val _line = line.rawValue
    val _column = column.rawValue
    val _offset = offset.rawValue
    val res = externals.clang_getSpellingLocation(_location, _file, _line, _column, _offset)
    return res
}

fun clang_getFileLocation(location: CXSourceLocation, file: CPointer<CXFileVar>?, line: CPointer<CInt32Var>?, column: CPointer<CInt32Var>?, offset: CPointer<CInt32Var>?): Unit {
    val _location = location.rawPtr
    val _file = file.rawValue
    val _line = line.rawValue
    val _column = column.rawValue
    val _offset = offset.rawValue
    val res = externals.clang_getFileLocation(_location, _file, _line, _column, _offset)
    return res
}

fun clang_getRangeStart(range: CXSourceRange, retValPlacement: NativePlacement): CXSourceLocation {
    val _range = range.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXSourceLocation>().rawPtr
    val res = externals.clang_getRangeStart(_range, _retValPlacement)
    return interpretPointed<CXSourceLocation>(res)
}

fun clang_getRangeEnd(range: CXSourceRange, retValPlacement: NativePlacement): CXSourceLocation {
    val _range = range.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXSourceLocation>().rawPtr
    val res = externals.clang_getRangeEnd(_range, _retValPlacement)
    return interpretPointed<CXSourceLocation>(res)
}

fun clang_getSkippedRanges(tu: CXTranslationUnit?, file: CXFile?): CPointer<CXSourceRangeList>? {
    val _tu = tu.rawValue
    val _file = file.rawValue
    val res = externals.clang_getSkippedRanges(_tu, _file)
    return CPointer.createNullable<CXSourceRangeList>(res)
}

fun clang_disposeSourceRangeList(ranges: CPointer<CXSourceRangeList>?): Unit {
    val _ranges = ranges.rawValue
    val res = externals.clang_disposeSourceRangeList(_ranges)
    return res
}

fun clang_getNumDiagnosticsInSet(Diags: CXDiagnosticSet?): Int {
    val _Diags = Diags.rawValue
    val res = externals.clang_getNumDiagnosticsInSet(_Diags)
    return res
}

fun clang_getDiagnosticInSet(Diags: CXDiagnosticSet?, Index: Int): CXDiagnostic? {
    val _Diags = Diags.rawValue
    val _Index = Index
    val res = externals.clang_getDiagnosticInSet(_Diags, _Index)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_loadDiagnostics(file: String?, error: CPointer<CXLoadDiag_Error.Var>?, errorString: CPointer<CXString>?): CXDiagnosticSet? {
    return memScoped {
        val _file = file?.toCString(memScope).rawPtr
        val _error = error.rawValue
        val _errorString = errorString.rawValue
        val res = externals.clang_loadDiagnostics(_file, _error, _errorString)
        CPointer.createNullable<COpaque>(res)
    }
}

fun clang_disposeDiagnosticSet(Diags: CXDiagnosticSet?): Unit {
    val _Diags = Diags.rawValue
    val res = externals.clang_disposeDiagnosticSet(_Diags)
    return res
}

fun clang_getChildDiagnostics(D: CXDiagnostic?): CXDiagnosticSet? {
    val _D = D.rawValue
    val res = externals.clang_getChildDiagnostics(_D)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_getNumDiagnostics(Unit: CXTranslationUnit?): Int {
    val _Unit = Unit.rawValue
    val res = externals.clang_getNumDiagnostics(_Unit)
    return res
}

fun clang_getDiagnostic(Unit: CXTranslationUnit?, Index: Int): CXDiagnostic? {
    val _Unit = Unit.rawValue
    val _Index = Index
    val res = externals.clang_getDiagnostic(_Unit, _Index)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_getDiagnosticSetFromTU(Unit: CXTranslationUnit?): CXDiagnosticSet? {
    val _Unit = Unit.rawValue
    val res = externals.clang_getDiagnosticSetFromTU(_Unit)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_disposeDiagnostic(Diagnostic: CXDiagnostic?): Unit {
    val _Diagnostic = Diagnostic.rawValue
    val res = externals.clang_disposeDiagnostic(_Diagnostic)
    return res
}

fun clang_formatDiagnostic(Diagnostic: CXDiagnostic?, Options: Int, retValPlacement: NativePlacement): CXString {
    val _Diagnostic = Diagnostic.rawValue
    val _Options = Options
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_formatDiagnostic(_Diagnostic, _Options, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_defaultDiagnosticDisplayOptions(): Int {
    val res = externals.clang_defaultDiagnosticDisplayOptions()
    return res
}

fun clang_getDiagnosticSeverity(arg0: CXDiagnostic?): CXDiagnosticSeverity {
    val _arg0 = arg0.rawValue
    val res = externals.clang_getDiagnosticSeverity(_arg0)
    return CXDiagnosticSeverity.byValue(res)
}

fun clang_getDiagnosticLocation(arg0: CXDiagnostic?, retValPlacement: NativePlacement): CXSourceLocation {
    val _arg0 = arg0.rawValue
    val _retValPlacement = retValPlacement.alloc<CXSourceLocation>().rawPtr
    val res = externals.clang_getDiagnosticLocation(_arg0, _retValPlacement)
    return interpretPointed<CXSourceLocation>(res)
}

fun clang_getDiagnosticSpelling(arg0: CXDiagnostic?, retValPlacement: NativePlacement): CXString {
    val _arg0 = arg0.rawValue
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getDiagnosticSpelling(_arg0, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getDiagnosticOption(Diag: CXDiagnostic?, Disable: CPointer<CXString>?, retValPlacement: NativePlacement): CXString {
    val _Diag = Diag.rawValue
    val _Disable = Disable.rawValue
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getDiagnosticOption(_Diag, _Disable, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getDiagnosticCategory(arg0: CXDiagnostic?): Int {
    val _arg0 = arg0.rawValue
    val res = externals.clang_getDiagnosticCategory(_arg0)
    return res
}

fun clang_getDiagnosticCategoryName(Category: Int, retValPlacement: NativePlacement): CXString {
    val _Category = Category
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getDiagnosticCategoryName(_Category, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getDiagnosticCategoryText(arg0: CXDiagnostic?, retValPlacement: NativePlacement): CXString {
    val _arg0 = arg0.rawValue
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getDiagnosticCategoryText(_arg0, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getDiagnosticNumRanges(arg0: CXDiagnostic?): Int {
    val _arg0 = arg0.rawValue
    val res = externals.clang_getDiagnosticNumRanges(_arg0)
    return res
}

fun clang_getDiagnosticRange(Diagnostic: CXDiagnostic?, Range: Int, retValPlacement: NativePlacement): CXSourceRange {
    val _Diagnostic = Diagnostic.rawValue
    val _Range = Range
    val _retValPlacement = retValPlacement.alloc<CXSourceRange>().rawPtr
    val res = externals.clang_getDiagnosticRange(_Diagnostic, _Range, _retValPlacement)
    return interpretPointed<CXSourceRange>(res)
}

fun clang_getDiagnosticNumFixIts(Diagnostic: CXDiagnostic?): Int {
    val _Diagnostic = Diagnostic.rawValue
    val res = externals.clang_getDiagnosticNumFixIts(_Diagnostic)
    return res
}

fun clang_getDiagnosticFixIt(Diagnostic: CXDiagnostic?, FixIt: Int, ReplacementRange: CPointer<CXSourceRange>?, retValPlacement: NativePlacement): CXString {
    val _Diagnostic = Diagnostic.rawValue
    val _FixIt = FixIt
    val _ReplacementRange = ReplacementRange.rawValue
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getDiagnosticFixIt(_Diagnostic, _FixIt, _ReplacementRange, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getTranslationUnitSpelling(CTUnit: CXTranslationUnit?, retValPlacement: NativePlacement): CXString {
    val _CTUnit = CTUnit.rawValue
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getTranslationUnitSpelling(_CTUnit, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_createTranslationUnitFromSourceFile(CIdx: CXIndex?, source_filename: String?, num_clang_command_line_args: Int, clang_command_line_args: CPointer<CPointerVar<CInt8Var>>?, num_unsaved_files: Int, unsaved_files: CPointer<CXUnsavedFile>?): CXTranslationUnit? {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _source_filename = source_filename?.toCString(memScope).rawPtr
        val _num_clang_command_line_args = num_clang_command_line_args
        val _clang_command_line_args = clang_command_line_args.rawValue
        val _num_unsaved_files = num_unsaved_files
        val _unsaved_files = unsaved_files.rawValue
        val res = externals.clang_createTranslationUnitFromSourceFile(_CIdx, _source_filename, _num_clang_command_line_args, _clang_command_line_args, _num_unsaved_files, _unsaved_files)
        CPointer.createNullable<CXTranslationUnitImpl>(res)
    }
}

fun clang_createTranslationUnit(CIdx: CXIndex?, ast_filename: String?): CXTranslationUnit? {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _ast_filename = ast_filename?.toCString(memScope).rawPtr
        val res = externals.clang_createTranslationUnit(_CIdx, _ast_filename)
        CPointer.createNullable<CXTranslationUnitImpl>(res)
    }
}

fun clang_createTranslationUnit2(CIdx: CXIndex?, ast_filename: String?, out_TU: CPointer<CXTranslationUnitVar>?): CXErrorCode {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _ast_filename = ast_filename?.toCString(memScope).rawPtr
        val _out_TU = out_TU.rawValue
        val res = externals.clang_createTranslationUnit2(_CIdx, _ast_filename, _out_TU)
        CXErrorCode.byValue(res)
    }
}

fun clang_defaultEditingTranslationUnitOptions(): Int {
    val res = externals.clang_defaultEditingTranslationUnitOptions()
    return res
}

fun clang_parseTranslationUnit(CIdx: CXIndex?, source_filename: String?, command_line_args: CPointer<CPointerVar<CInt8Var>>?, num_command_line_args: Int, unsaved_files: CPointer<CXUnsavedFile>?, num_unsaved_files: Int, options: Int): CXTranslationUnit? {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _source_filename = source_filename?.toCString(memScope).rawPtr
        val _command_line_args = command_line_args.rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files.rawValue
        val _num_unsaved_files = num_unsaved_files
        val _options = options
        val res = externals.clang_parseTranslationUnit(_CIdx, _source_filename, _command_line_args, _num_command_line_args, _unsaved_files, _num_unsaved_files, _options)
        CPointer.createNullable<CXTranslationUnitImpl>(res)
    }
}

fun clang_parseTranslationUnit2(CIdx: CXIndex?, source_filename: String?, command_line_args: CPointer<CPointerVar<CInt8Var>>?, num_command_line_args: Int, unsaved_files: CPointer<CXUnsavedFile>?, num_unsaved_files: Int, options: Int, out_TU: CPointer<CXTranslationUnitVar>?): CXErrorCode {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _source_filename = source_filename?.toCString(memScope).rawPtr
        val _command_line_args = command_line_args.rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files.rawValue
        val _num_unsaved_files = num_unsaved_files
        val _options = options
        val _out_TU = out_TU.rawValue
        val res = externals.clang_parseTranslationUnit2(_CIdx, _source_filename, _command_line_args, _num_command_line_args, _unsaved_files, _num_unsaved_files, _options, _out_TU)
        CXErrorCode.byValue(res)
    }
}

fun clang_parseTranslationUnit2FullArgv(CIdx: CXIndex?, source_filename: String?, command_line_args: CPointer<CPointerVar<CInt8Var>>?, num_command_line_args: Int, unsaved_files: CPointer<CXUnsavedFile>?, num_unsaved_files: Int, options: Int, out_TU: CPointer<CXTranslationUnitVar>?): CXErrorCode {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _source_filename = source_filename?.toCString(memScope).rawPtr
        val _command_line_args = command_line_args.rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files.rawValue
        val _num_unsaved_files = num_unsaved_files
        val _options = options
        val _out_TU = out_TU.rawValue
        val res = externals.clang_parseTranslationUnit2FullArgv(_CIdx, _source_filename, _command_line_args, _num_command_line_args, _unsaved_files, _num_unsaved_files, _options, _out_TU)
        CXErrorCode.byValue(res)
    }
}

fun clang_defaultSaveOptions(TU: CXTranslationUnit?): Int {
    val _TU = TU.rawValue
    val res = externals.clang_defaultSaveOptions(_TU)
    return res
}

fun clang_saveTranslationUnit(TU: CXTranslationUnit?, FileName: String?, options: Int): Int {
    return memScoped {
        val _TU = TU.rawValue
        val _FileName = FileName?.toCString(memScope).rawPtr
        val _options = options
        val res = externals.clang_saveTranslationUnit(_TU, _FileName, _options)
        res
    }
}

fun clang_disposeTranslationUnit(arg0: CXTranslationUnit?): Unit {
    val _arg0 = arg0.rawValue
    val res = externals.clang_disposeTranslationUnit(_arg0)
    return res
}

fun clang_defaultReparseOptions(TU: CXTranslationUnit?): Int {
    val _TU = TU.rawValue
    val res = externals.clang_defaultReparseOptions(_TU)
    return res
}

fun clang_reparseTranslationUnit(TU: CXTranslationUnit?, num_unsaved_files: Int, unsaved_files: CPointer<CXUnsavedFile>?, options: Int): Int {
    val _TU = TU.rawValue
    val _num_unsaved_files = num_unsaved_files
    val _unsaved_files = unsaved_files.rawValue
    val _options = options
    val res = externals.clang_reparseTranslationUnit(_TU, _num_unsaved_files, _unsaved_files, _options)
    return res
}

fun clang_getTUResourceUsageName(kind: CXTUResourceUsageKind): CPointer<CInt8Var>? {
    val _kind = kind.value
    val res = externals.clang_getTUResourceUsageName(_kind)
    return CPointer.createNullable<CInt8Var>(res)
}

fun clang_getCXTUResourceUsage(TU: CXTranslationUnit?, retValPlacement: NativePlacement): CXTUResourceUsage {
    val _TU = TU.rawValue
    val _retValPlacement = retValPlacement.alloc<CXTUResourceUsage>().rawPtr
    val res = externals.clang_getCXTUResourceUsage(_TU, _retValPlacement)
    return interpretPointed<CXTUResourceUsage>(res)
}

fun clang_disposeCXTUResourceUsage(usage: CXTUResourceUsage): Unit {
    val _usage = usage.rawPtr
    val res = externals.clang_disposeCXTUResourceUsage(_usage)
    return res
}

fun clang_getNullCursor(retValPlacement: NativePlacement): CXCursor {
    val _retValPlacement = retValPlacement.alloc<CXCursor>().rawPtr
    val res = externals.clang_getNullCursor(_retValPlacement)
    return interpretPointed<CXCursor>(res)
}

fun clang_getTranslationUnitCursor(arg0: CXTranslationUnit?, retValPlacement: NativePlacement): CXCursor {
    val _arg0 = arg0.rawValue
    val _retValPlacement = retValPlacement.alloc<CXCursor>().rawPtr
    val res = externals.clang_getTranslationUnitCursor(_arg0, _retValPlacement)
    return interpretPointed<CXCursor>(res)
}

fun clang_equalCursors(arg0: CXCursor, arg1: CXCursor): Int {
    val _arg0 = arg0.rawPtr
    val _arg1 = arg1.rawPtr
    val res = externals.clang_equalCursors(_arg0, _arg1)
    return res
}

fun clang_Cursor_isNull(cursor: CXCursor): Int {
    val _cursor = cursor.rawPtr
    val res = externals.clang_Cursor_isNull(_cursor)
    return res
}

fun clang_hashCursor(arg0: CXCursor): Int {
    val _arg0 = arg0.rawPtr
    val res = externals.clang_hashCursor(_arg0)
    return res
}

fun clang_getCursorKind(arg0: CXCursor): CXCursorKind {
    val _arg0 = arg0.rawPtr
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

fun clang_Cursor_hasAttrs(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_hasAttrs(_C)
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
    val _cursor = cursor.rawPtr
    val res = externals.clang_getCursorLinkage(_cursor)
    return CXLinkageKind.byValue(res)
}

fun clang_getCursorVisibility(cursor: CXCursor): CXVisibilityKind {
    val _cursor = cursor.rawPtr
    val res = externals.clang_getCursorVisibility(_cursor)
    return CXVisibilityKind.byValue(res)
}

fun clang_getCursorAvailability(cursor: CXCursor): CXAvailabilityKind {
    val _cursor = cursor.rawPtr
    val res = externals.clang_getCursorAvailability(_cursor)
    return CXAvailabilityKind.byValue(res)
}

fun clang_getCursorPlatformAvailability(cursor: CXCursor, always_deprecated: CPointer<CInt32Var>?, deprecated_message: CPointer<CXString>?, always_unavailable: CPointer<CInt32Var>?, unavailable_message: CPointer<CXString>?, availability: CPointer<CXPlatformAvailability>?, availability_size: Int): Int {
    val _cursor = cursor.rawPtr
    val _always_deprecated = always_deprecated.rawValue
    val _deprecated_message = deprecated_message.rawValue
    val _always_unavailable = always_unavailable.rawValue
    val _unavailable_message = unavailable_message.rawValue
    val _availability = availability.rawValue
    val _availability_size = availability_size
    val res = externals.clang_getCursorPlatformAvailability(_cursor, _always_deprecated, _deprecated_message, _always_unavailable, _unavailable_message, _availability, _availability_size)
    return res
}

fun clang_disposeCXPlatformAvailability(availability: CPointer<CXPlatformAvailability>?): Unit {
    val _availability = availability.rawValue
    val res = externals.clang_disposeCXPlatformAvailability(_availability)
    return res
}

fun clang_getCursorLanguage(cursor: CXCursor): CXLanguageKind {
    val _cursor = cursor.rawPtr
    val res = externals.clang_getCursorLanguage(_cursor)
    return CXLanguageKind.byValue(res)
}

fun clang_Cursor_getTranslationUnit(arg0: CXCursor): CXTranslationUnit? {
    val _arg0 = arg0.rawPtr
    val res = externals.clang_Cursor_getTranslationUnit(_arg0)
    return CPointer.createNullable<CXTranslationUnitImpl>(res)
}

fun clang_createCXCursorSet(): CXCursorSet? {
    val res = externals.clang_createCXCursorSet()
    return CPointer.createNullable<CXCursorSetImpl>(res)
}

fun clang_disposeCXCursorSet(cset: CXCursorSet?): Unit {
    val _cset = cset.rawValue
    val res = externals.clang_disposeCXCursorSet(_cset)
    return res
}

fun clang_CXCursorSet_contains(cset: CXCursorSet?, cursor: CXCursor): Int {
    val _cset = cset.rawValue
    val _cursor = cursor.rawPtr
    val res = externals.clang_CXCursorSet_contains(_cset, _cursor)
    return res
}

fun clang_CXCursorSet_insert(cset: CXCursorSet?, cursor: CXCursor): Int {
    val _cset = cset.rawValue
    val _cursor = cursor.rawPtr
    val res = externals.clang_CXCursorSet_insert(_cset, _cursor)
    return res
}

fun clang_getCursorSemanticParent(cursor: CXCursor, retValPlacement: NativePlacement): CXCursor {
    val _cursor = cursor.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXCursor>().rawPtr
    val res = externals.clang_getCursorSemanticParent(_cursor, _retValPlacement)
    return interpretPointed<CXCursor>(res)
}

fun clang_getCursorLexicalParent(cursor: CXCursor, retValPlacement: NativePlacement): CXCursor {
    val _cursor = cursor.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXCursor>().rawPtr
    val res = externals.clang_getCursorLexicalParent(_cursor, _retValPlacement)
    return interpretPointed<CXCursor>(res)
}

fun clang_getOverriddenCursors(cursor: CXCursor, overridden: CPointer<CPointerVar<CXCursor>>?, num_overridden: CPointer<CInt32Var>?): Unit {
    val _cursor = cursor.rawPtr
    val _overridden = overridden.rawValue
    val _num_overridden = num_overridden.rawValue
    val res = externals.clang_getOverriddenCursors(_cursor, _overridden, _num_overridden)
    return res
}

fun clang_disposeOverriddenCursors(overridden: CPointer<CXCursor>?): Unit {
    val _overridden = overridden.rawValue
    val res = externals.clang_disposeOverriddenCursors(_overridden)
    return res
}

fun clang_getIncludedFile(cursor: CXCursor): CXFile? {
    val _cursor = cursor.rawPtr
    val res = externals.clang_getIncludedFile(_cursor)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_getCursor(arg0: CXTranslationUnit?, arg1: CXSourceLocation, retValPlacement: NativePlacement): CXCursor {
    val _arg0 = arg0.rawValue
    val _arg1 = arg1.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXCursor>().rawPtr
    val res = externals.clang_getCursor(_arg0, _arg1, _retValPlacement)
    return interpretPointed<CXCursor>(res)
}

fun clang_getCursorLocation(arg0: CXCursor, retValPlacement: NativePlacement): CXSourceLocation {
    val _arg0 = arg0.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXSourceLocation>().rawPtr
    val res = externals.clang_getCursorLocation(_arg0, _retValPlacement)
    return interpretPointed<CXSourceLocation>(res)
}

fun clang_getCursorExtent(arg0: CXCursor, retValPlacement: NativePlacement): CXSourceRange {
    val _arg0 = arg0.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXSourceRange>().rawPtr
    val res = externals.clang_getCursorExtent(_arg0, _retValPlacement)
    return interpretPointed<CXSourceRange>(res)
}

fun clang_getCursorType(C: CXCursor, retValPlacement: NativePlacement): CXType {
    val _C = C.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_getCursorType(_C, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_getTypeSpelling(CT: CXType, retValPlacement: NativePlacement): CXString {
    val _CT = CT.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getTypeSpelling(_CT, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getTypedefDeclUnderlyingType(C: CXCursor, retValPlacement: NativePlacement): CXType {
    val _C = C.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_getTypedefDeclUnderlyingType(_C, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_getEnumDeclIntegerType(C: CXCursor, retValPlacement: NativePlacement): CXType {
    val _C = C.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_getEnumDeclIntegerType(_C, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_getEnumConstantDeclValue(C: CXCursor): Long {
    val _C = C.rawPtr
    val res = externals.clang_getEnumConstantDeclValue(_C)
    return res
}

fun clang_getEnumConstantDeclUnsignedValue(C: CXCursor): Long {
    val _C = C.rawPtr
    val res = externals.clang_getEnumConstantDeclUnsignedValue(_C)
    return res
}

fun clang_getFieldDeclBitWidth(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_getFieldDeclBitWidth(_C)
    return res
}

fun clang_Cursor_getNumArguments(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_getNumArguments(_C)
    return res
}

fun clang_Cursor_getArgument(C: CXCursor, i: Int, retValPlacement: NativePlacement): CXCursor {
    val _C = C.rawPtr
    val _i = i
    val _retValPlacement = retValPlacement.alloc<CXCursor>().rawPtr
    val res = externals.clang_Cursor_getArgument(_C, _i, _retValPlacement)
    return interpretPointed<CXCursor>(res)
}

fun clang_Cursor_getNumTemplateArguments(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_getNumTemplateArguments(_C)
    return res
}

fun clang_Cursor_getTemplateArgumentKind(C: CXCursor, I: Int): CXTemplateArgumentKind {
    val _C = C.rawPtr
    val _I = I
    val res = externals.clang_Cursor_getTemplateArgumentKind(_C, _I)
    return CXTemplateArgumentKind.byValue(res)
}

fun clang_Cursor_getTemplateArgumentType(C: CXCursor, I: Int, retValPlacement: NativePlacement): CXType {
    val _C = C.rawPtr
    val _I = I
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_Cursor_getTemplateArgumentType(_C, _I, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_Cursor_getTemplateArgumentValue(C: CXCursor, I: Int): Long {
    val _C = C.rawPtr
    val _I = I
    val res = externals.clang_Cursor_getTemplateArgumentValue(_C, _I)
    return res
}

fun clang_Cursor_getTemplateArgumentUnsignedValue(C: CXCursor, I: Int): Long {
    val _C = C.rawPtr
    val _I = I
    val res = externals.clang_Cursor_getTemplateArgumentUnsignedValue(_C, _I)
    return res
}

fun clang_equalTypes(A: CXType, B: CXType): Int {
    val _A = A.rawPtr
    val _B = B.rawPtr
    val res = externals.clang_equalTypes(_A, _B)
    return res
}

fun clang_getCanonicalType(T: CXType, retValPlacement: NativePlacement): CXType {
    val _T = T.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_getCanonicalType(_T, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_isConstQualifiedType(T: CXType): Int {
    val _T = T.rawPtr
    val res = externals.clang_isConstQualifiedType(_T)
    return res
}

fun clang_Cursor_isMacroFunctionLike(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_isMacroFunctionLike(_C)
    return res
}

fun clang_Cursor_isMacroBuiltin(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_isMacroBuiltin(_C)
    return res
}

fun clang_Cursor_isFunctionInlined(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_isFunctionInlined(_C)
    return res
}

fun clang_isVolatileQualifiedType(T: CXType): Int {
    val _T = T.rawPtr
    val res = externals.clang_isVolatileQualifiedType(_T)
    return res
}

fun clang_isRestrictQualifiedType(T: CXType): Int {
    val _T = T.rawPtr
    val res = externals.clang_isRestrictQualifiedType(_T)
    return res
}

fun clang_getPointeeType(T: CXType, retValPlacement: NativePlacement): CXType {
    val _T = T.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_getPointeeType(_T, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_getTypeDeclaration(T: CXType, retValPlacement: NativePlacement): CXCursor {
    val _T = T.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXCursor>().rawPtr
    val res = externals.clang_getTypeDeclaration(_T, _retValPlacement)
    return interpretPointed<CXCursor>(res)
}

fun clang_getDeclObjCTypeEncoding(C: CXCursor, retValPlacement: NativePlacement): CXString {
    val _C = C.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getDeclObjCTypeEncoding(_C, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_Type_getObjCEncoding(type: CXType, retValPlacement: NativePlacement): CXString {
    val _type = type.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_Type_getObjCEncoding(_type, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getTypeKindSpelling(K: CXTypeKind, retValPlacement: NativePlacement): CXString {
    val _K = K.value
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getTypeKindSpelling(_K, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getFunctionTypeCallingConv(T: CXType): CXCallingConv {
    val _T = T.rawPtr
    val res = externals.clang_getFunctionTypeCallingConv(_T)
    return CXCallingConv.byValue(res)
}

fun clang_getResultType(T: CXType, retValPlacement: NativePlacement): CXType {
    val _T = T.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_getResultType(_T, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_getNumArgTypes(T: CXType): Int {
    val _T = T.rawPtr
    val res = externals.clang_getNumArgTypes(_T)
    return res
}

fun clang_getArgType(T: CXType, i: Int, retValPlacement: NativePlacement): CXType {
    val _T = T.rawPtr
    val _i = i
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_getArgType(_T, _i, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_isFunctionTypeVariadic(T: CXType): Int {
    val _T = T.rawPtr
    val res = externals.clang_isFunctionTypeVariadic(_T)
    return res
}

fun clang_getCursorResultType(C: CXCursor, retValPlacement: NativePlacement): CXType {
    val _C = C.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_getCursorResultType(_C, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_isPODType(T: CXType): Int {
    val _T = T.rawPtr
    val res = externals.clang_isPODType(_T)
    return res
}

fun clang_getElementType(T: CXType, retValPlacement: NativePlacement): CXType {
    val _T = T.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_getElementType(_T, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_getNumElements(T: CXType): Long {
    val _T = T.rawPtr
    val res = externals.clang_getNumElements(_T)
    return res
}

fun clang_getArrayElementType(T: CXType, retValPlacement: NativePlacement): CXType {
    val _T = T.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_getArrayElementType(_T, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_getArraySize(T: CXType): Long {
    val _T = T.rawPtr
    val res = externals.clang_getArraySize(_T)
    return res
}

fun clang_Type_getNamedType(T: CXType, retValPlacement: NativePlacement): CXType {
    val _T = T.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_Type_getNamedType(_T, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_Type_getAlignOf(T: CXType): Long {
    val _T = T.rawPtr
    val res = externals.clang_Type_getAlignOf(_T)
    return res
}

fun clang_Type_getClassType(T: CXType, retValPlacement: NativePlacement): CXType {
    val _T = T.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_Type_getClassType(_T, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_Type_getSizeOf(T: CXType): Long {
    val _T = T.rawPtr
    val res = externals.clang_Type_getSizeOf(_T)
    return res
}

fun clang_Type_getOffsetOf(T: CXType, S: String?): Long {
    return memScoped {
        val _T = T.rawPtr
        val _S = S?.toCString(memScope).rawPtr
        val res = externals.clang_Type_getOffsetOf(_T, _S)
        res
    }
}

fun clang_Cursor_getOffsetOfField(C: CXCursor): Long {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_getOffsetOfField(_C)
    return res
}

fun clang_Cursor_isAnonymous(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_isAnonymous(_C)
    return res
}

fun clang_Type_getNumTemplateArguments(T: CXType): Int {
    val _T = T.rawPtr
    val res = externals.clang_Type_getNumTemplateArguments(_T)
    return res
}

fun clang_Type_getTemplateArgumentAsType(T: CXType, i: Int, retValPlacement: NativePlacement): CXType {
    val _T = T.rawPtr
    val _i = i
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_Type_getTemplateArgumentAsType(_T, _i, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_Type_getCXXRefQualifier(T: CXType): CXRefQualifierKind {
    val _T = T.rawPtr
    val res = externals.clang_Type_getCXXRefQualifier(_T)
    return CXRefQualifierKind.byValue(res)
}

fun clang_Cursor_isBitField(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_isBitField(_C)
    return res
}

fun clang_isVirtualBase(arg0: CXCursor): Int {
    val _arg0 = arg0.rawPtr
    val res = externals.clang_isVirtualBase(_arg0)
    return res
}

fun clang_getCXXAccessSpecifier(arg0: CXCursor): CX_CXXAccessSpecifier {
    val _arg0 = arg0.rawPtr
    val res = externals.clang_getCXXAccessSpecifier(_arg0)
    return CX_CXXAccessSpecifier.byValue(res)
}

fun clang_Cursor_getStorageClass(arg0: CXCursor): CX_StorageClass {
    val _arg0 = arg0.rawPtr
    val res = externals.clang_Cursor_getStorageClass(_arg0)
    return CX_StorageClass.byValue(res)
}

fun clang_getNumOverloadedDecls(cursor: CXCursor): Int {
    val _cursor = cursor.rawPtr
    val res = externals.clang_getNumOverloadedDecls(_cursor)
    return res
}

fun clang_getOverloadedDecl(cursor: CXCursor, index: Int, retValPlacement: NativePlacement): CXCursor {
    val _cursor = cursor.rawPtr
    val _index = index
    val _retValPlacement = retValPlacement.alloc<CXCursor>().rawPtr
    val res = externals.clang_getOverloadedDecl(_cursor, _index, _retValPlacement)
    return interpretPointed<CXCursor>(res)
}

fun clang_getIBOutletCollectionType(arg0: CXCursor, retValPlacement: NativePlacement): CXType {
    val _arg0 = arg0.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_getIBOutletCollectionType(_arg0, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_visitChildren(parent: CXCursor, visitor: CXCursorVisitor?, client_data: CXClientData?): Int {
    val _parent = parent.rawPtr
    val _visitor = visitor.rawValue
    val _client_data = client_data.rawValue
    val res = externals.clang_visitChildren(_parent, _visitor, _client_data)
    return res
}

fun clang_getCursorUSR(arg0: CXCursor, retValPlacement: NativePlacement): CXString {
    val _arg0 = arg0.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getCursorUSR(_arg0, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_constructUSR_ObjCClass(class_name: String?, retValPlacement: NativePlacement): CXString {
    return memScoped {
        val _class_name = class_name?.toCString(memScope).rawPtr
        val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
        val res = externals.clang_constructUSR_ObjCClass(_class_name, _retValPlacement)
        interpretPointed<CXString>(res)
    }
}

fun clang_constructUSR_ObjCCategory(class_name: String?, category_name: String?, retValPlacement: NativePlacement): CXString {
    return memScoped {
        val _class_name = class_name?.toCString(memScope).rawPtr
        val _category_name = category_name?.toCString(memScope).rawPtr
        val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
        val res = externals.clang_constructUSR_ObjCCategory(_class_name, _category_name, _retValPlacement)
        interpretPointed<CXString>(res)
    }
}

fun clang_constructUSR_ObjCProtocol(protocol_name: String?, retValPlacement: NativePlacement): CXString {
    return memScoped {
        val _protocol_name = protocol_name?.toCString(memScope).rawPtr
        val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
        val res = externals.clang_constructUSR_ObjCProtocol(_protocol_name, _retValPlacement)
        interpretPointed<CXString>(res)
    }
}

fun clang_constructUSR_ObjCIvar(name: String?, classUSR: CXString, retValPlacement: NativePlacement): CXString {
    return memScoped {
        val _name = name?.toCString(memScope).rawPtr
        val _classUSR = classUSR.rawPtr
        val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
        val res = externals.clang_constructUSR_ObjCIvar(_name, _classUSR, _retValPlacement)
        interpretPointed<CXString>(res)
    }
}

fun clang_constructUSR_ObjCMethod(name: String?, isInstanceMethod: Int, classUSR: CXString, retValPlacement: NativePlacement): CXString {
    return memScoped {
        val _name = name?.toCString(memScope).rawPtr
        val _isInstanceMethod = isInstanceMethod
        val _classUSR = classUSR.rawPtr
        val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
        val res = externals.clang_constructUSR_ObjCMethod(_name, _isInstanceMethod, _classUSR, _retValPlacement)
        interpretPointed<CXString>(res)
    }
}

fun clang_constructUSR_ObjCProperty(property: String?, classUSR: CXString, retValPlacement: NativePlacement): CXString {
    return memScoped {
        val _property = property?.toCString(memScope).rawPtr
        val _classUSR = classUSR.rawPtr
        val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
        val res = externals.clang_constructUSR_ObjCProperty(_property, _classUSR, _retValPlacement)
        interpretPointed<CXString>(res)
    }
}

fun clang_getCursorSpelling(arg0: CXCursor, retValPlacement: NativePlacement): CXString {
    val _arg0 = arg0.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getCursorSpelling(_arg0, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_Cursor_getSpellingNameRange(arg0: CXCursor, pieceIndex: Int, options: Int, retValPlacement: NativePlacement): CXSourceRange {
    val _arg0 = arg0.rawPtr
    val _pieceIndex = pieceIndex
    val _options = options
    val _retValPlacement = retValPlacement.alloc<CXSourceRange>().rawPtr
    val res = externals.clang_Cursor_getSpellingNameRange(_arg0, _pieceIndex, _options, _retValPlacement)
    return interpretPointed<CXSourceRange>(res)
}

fun clang_getCursorDisplayName(arg0: CXCursor, retValPlacement: NativePlacement): CXString {
    val _arg0 = arg0.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getCursorDisplayName(_arg0, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getCursorReferenced(arg0: CXCursor, retValPlacement: NativePlacement): CXCursor {
    val _arg0 = arg0.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXCursor>().rawPtr
    val res = externals.clang_getCursorReferenced(_arg0, _retValPlacement)
    return interpretPointed<CXCursor>(res)
}

fun clang_getCursorDefinition(arg0: CXCursor, retValPlacement: NativePlacement): CXCursor {
    val _arg0 = arg0.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXCursor>().rawPtr
    val res = externals.clang_getCursorDefinition(_arg0, _retValPlacement)
    return interpretPointed<CXCursor>(res)
}

fun clang_isCursorDefinition(arg0: CXCursor): Int {
    val _arg0 = arg0.rawPtr
    val res = externals.clang_isCursorDefinition(_arg0)
    return res
}

fun clang_getCanonicalCursor(arg0: CXCursor, retValPlacement: NativePlacement): CXCursor {
    val _arg0 = arg0.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXCursor>().rawPtr
    val res = externals.clang_getCanonicalCursor(_arg0, _retValPlacement)
    return interpretPointed<CXCursor>(res)
}

fun clang_Cursor_getObjCSelectorIndex(arg0: CXCursor): Int {
    val _arg0 = arg0.rawPtr
    val res = externals.clang_Cursor_getObjCSelectorIndex(_arg0)
    return res
}

fun clang_Cursor_isDynamicCall(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_isDynamicCall(_C)
    return res
}

fun clang_Cursor_getReceiverType(C: CXCursor, retValPlacement: NativePlacement): CXType {
    val _C = C.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXType>().rawPtr
    val res = externals.clang_Cursor_getReceiverType(_C, _retValPlacement)
    return interpretPointed<CXType>(res)
}

fun clang_Cursor_getObjCPropertyAttributes(C: CXCursor, reserved: Int): Int {
    val _C = C.rawPtr
    val _reserved = reserved
    val res = externals.clang_Cursor_getObjCPropertyAttributes(_C, _reserved)
    return res
}

fun clang_Cursor_getObjCDeclQualifiers(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_getObjCDeclQualifiers(_C)
    return res
}

fun clang_Cursor_isObjCOptional(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_isObjCOptional(_C)
    return res
}

fun clang_Cursor_isVariadic(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_isVariadic(_C)
    return res
}

fun clang_Cursor_getCommentRange(C: CXCursor, retValPlacement: NativePlacement): CXSourceRange {
    val _C = C.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXSourceRange>().rawPtr
    val res = externals.clang_Cursor_getCommentRange(_C, _retValPlacement)
    return interpretPointed<CXSourceRange>(res)
}

fun clang_Cursor_getRawCommentText(C: CXCursor, retValPlacement: NativePlacement): CXString {
    val _C = C.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_Cursor_getRawCommentText(_C, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_Cursor_getBriefCommentText(C: CXCursor, retValPlacement: NativePlacement): CXString {
    val _C = C.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_Cursor_getBriefCommentText(_C, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_Cursor_getMangling(arg0: CXCursor, retValPlacement: NativePlacement): CXString {
    val _arg0 = arg0.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_Cursor_getMangling(_arg0, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_Cursor_getCXXManglings(arg0: CXCursor): CPointer<CXStringSet>? {
    val _arg0 = arg0.rawPtr
    val res = externals.clang_Cursor_getCXXManglings(_arg0)
    return CPointer.createNullable<CXStringSet>(res)
}

fun clang_Cursor_getModule(C: CXCursor): CXModule? {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_getModule(_C)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_getModuleForFile(arg0: CXTranslationUnit?, arg1: CXFile?): CXModule? {
    val _arg0 = arg0.rawValue
    val _arg1 = arg1.rawValue
    val res = externals.clang_getModuleForFile(_arg0, _arg1)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_Module_getASTFile(Module: CXModule?): CXFile? {
    val _Module = Module.rawValue
    val res = externals.clang_Module_getASTFile(_Module)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_Module_getParent(Module: CXModule?): CXModule? {
    val _Module = Module.rawValue
    val res = externals.clang_Module_getParent(_Module)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_Module_getName(Module: CXModule?, retValPlacement: NativePlacement): CXString {
    val _Module = Module.rawValue
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_Module_getName(_Module, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_Module_getFullName(Module: CXModule?, retValPlacement: NativePlacement): CXString {
    val _Module = Module.rawValue
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_Module_getFullName(_Module, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_Module_isSystem(Module: CXModule?): Int {
    val _Module = Module.rawValue
    val res = externals.clang_Module_isSystem(_Module)
    return res
}

fun clang_Module_getNumTopLevelHeaders(arg0: CXTranslationUnit?, Module: CXModule?): Int {
    val _arg0 = arg0.rawValue
    val _Module = Module.rawValue
    val res = externals.clang_Module_getNumTopLevelHeaders(_arg0, _Module)
    return res
}

fun clang_Module_getTopLevelHeader(arg0: CXTranslationUnit?, Module: CXModule?, Index: Int): CXFile? {
    val _arg0 = arg0.rawValue
    val _Module = Module.rawValue
    val _Index = Index
    val res = externals.clang_Module_getTopLevelHeader(_arg0, _Module, _Index)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_CXXConstructor_isConvertingConstructor(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_CXXConstructor_isConvertingConstructor(_C)
    return res
}

fun clang_CXXConstructor_isCopyConstructor(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_CXXConstructor_isCopyConstructor(_C)
    return res
}

fun clang_CXXConstructor_isDefaultConstructor(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_CXXConstructor_isDefaultConstructor(_C)
    return res
}

fun clang_CXXConstructor_isMoveConstructor(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_CXXConstructor_isMoveConstructor(_C)
    return res
}

fun clang_CXXField_isMutable(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_CXXField_isMutable(_C)
    return res
}

fun clang_CXXMethod_isDefaulted(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_CXXMethod_isDefaulted(_C)
    return res
}

fun clang_CXXMethod_isPureVirtual(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_CXXMethod_isPureVirtual(_C)
    return res
}

fun clang_CXXMethod_isStatic(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_CXXMethod_isStatic(_C)
    return res
}

fun clang_CXXMethod_isVirtual(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_CXXMethod_isVirtual(_C)
    return res
}

fun clang_CXXMethod_isConst(C: CXCursor): Int {
    val _C = C.rawPtr
    val res = externals.clang_CXXMethod_isConst(_C)
    return res
}

fun clang_getTemplateCursorKind(C: CXCursor): CXCursorKind {
    val _C = C.rawPtr
    val res = externals.clang_getTemplateCursorKind(_C)
    return CXCursorKind.byValue(res)
}

fun clang_getSpecializedCursorTemplate(C: CXCursor, retValPlacement: NativePlacement): CXCursor {
    val _C = C.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXCursor>().rawPtr
    val res = externals.clang_getSpecializedCursorTemplate(_C, _retValPlacement)
    return interpretPointed<CXCursor>(res)
}

fun clang_getCursorReferenceNameRange(C: CXCursor, NameFlags: Int, PieceIndex: Int, retValPlacement: NativePlacement): CXSourceRange {
    val _C = C.rawPtr
    val _NameFlags = NameFlags
    val _PieceIndex = PieceIndex
    val _retValPlacement = retValPlacement.alloc<CXSourceRange>().rawPtr
    val res = externals.clang_getCursorReferenceNameRange(_C, _NameFlags, _PieceIndex, _retValPlacement)
    return interpretPointed<CXSourceRange>(res)
}

fun clang_getTokenKind(arg0: CXToken): CXTokenKind {
    val _arg0 = arg0.rawPtr
    val res = externals.clang_getTokenKind(_arg0)
    return CXTokenKind.byValue(res)
}

fun clang_getTokenSpelling(arg0: CXTranslationUnit?, arg1: CXToken, retValPlacement: NativePlacement): CXString {
    val _arg0 = arg0.rawValue
    val _arg1 = arg1.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getTokenSpelling(_arg0, _arg1, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getTokenLocation(arg0: CXTranslationUnit?, arg1: CXToken, retValPlacement: NativePlacement): CXSourceLocation {
    val _arg0 = arg0.rawValue
    val _arg1 = arg1.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXSourceLocation>().rawPtr
    val res = externals.clang_getTokenLocation(_arg0, _arg1, _retValPlacement)
    return interpretPointed<CXSourceLocation>(res)
}

fun clang_getTokenExtent(arg0: CXTranslationUnit?, arg1: CXToken, retValPlacement: NativePlacement): CXSourceRange {
    val _arg0 = arg0.rawValue
    val _arg1 = arg1.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXSourceRange>().rawPtr
    val res = externals.clang_getTokenExtent(_arg0, _arg1, _retValPlacement)
    return interpretPointed<CXSourceRange>(res)
}

fun clang_tokenize(TU: CXTranslationUnit?, Range: CXSourceRange, Tokens: CPointer<CPointerVar<CXToken>>?, NumTokens: CPointer<CInt32Var>?): Unit {
    val _TU = TU.rawValue
    val _Range = Range.rawPtr
    val _Tokens = Tokens.rawValue
    val _NumTokens = NumTokens.rawValue
    val res = externals.clang_tokenize(_TU, _Range, _Tokens, _NumTokens)
    return res
}

fun clang_annotateTokens(TU: CXTranslationUnit?, Tokens: CPointer<CXToken>?, NumTokens: Int, Cursors: CPointer<CXCursor>?): Unit {
    val _TU = TU.rawValue
    val _Tokens = Tokens.rawValue
    val _NumTokens = NumTokens
    val _Cursors = Cursors.rawValue
    val res = externals.clang_annotateTokens(_TU, _Tokens, _NumTokens, _Cursors)
    return res
}

fun clang_disposeTokens(TU: CXTranslationUnit?, Tokens: CPointer<CXToken>?, NumTokens: Int): Unit {
    val _TU = TU.rawValue
    val _Tokens = Tokens.rawValue
    val _NumTokens = NumTokens
    val res = externals.clang_disposeTokens(_TU, _Tokens, _NumTokens)
    return res
}

fun clang_getCursorKindSpelling(Kind: CXCursorKind, retValPlacement: NativePlacement): CXString {
    val _Kind = Kind.value
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getCursorKindSpelling(_Kind, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getDefinitionSpellingAndExtent(arg0: CXCursor, startBuf: CPointer<CPointerVar<CInt8Var>>?, endBuf: CPointer<CPointerVar<CInt8Var>>?, startLine: CPointer<CInt32Var>?, startColumn: CPointer<CInt32Var>?, endLine: CPointer<CInt32Var>?, endColumn: CPointer<CInt32Var>?): Unit {
    val _arg0 = arg0.rawPtr
    val _startBuf = startBuf.rawValue
    val _endBuf = endBuf.rawValue
    val _startLine = startLine.rawValue
    val _startColumn = startColumn.rawValue
    val _endLine = endLine.rawValue
    val _endColumn = endColumn.rawValue
    val res = externals.clang_getDefinitionSpellingAndExtent(_arg0, _startBuf, _endBuf, _startLine, _startColumn, _endLine, _endColumn)
    return res
}

fun clang_enableStackTraces(): Unit {
    val res = externals.clang_enableStackTraces()
    return res
}

fun clang_executeOnThread(fn: CFunctionPointer<CFunctionType2>?, user_data: COpaquePointer?, stack_size: Int): Unit {
    val _fn = fn.rawValue
    val _user_data = user_data.rawValue
    val _stack_size = stack_size
    val res = externals.clang_executeOnThread(_fn, _user_data, _stack_size)
    return res
}

fun clang_getCompletionChunkKind(completion_string: CXCompletionString?, chunk_number: Int): CXCompletionChunkKind {
    val _completion_string = completion_string.rawValue
    val _chunk_number = chunk_number
    val res = externals.clang_getCompletionChunkKind(_completion_string, _chunk_number)
    return CXCompletionChunkKind.byValue(res)
}

fun clang_getCompletionChunkText(completion_string: CXCompletionString?, chunk_number: Int, retValPlacement: NativePlacement): CXString {
    val _completion_string = completion_string.rawValue
    val _chunk_number = chunk_number
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getCompletionChunkText(_completion_string, _chunk_number, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getCompletionChunkCompletionString(completion_string: CXCompletionString?, chunk_number: Int): CXCompletionString? {
    val _completion_string = completion_string.rawValue
    val _chunk_number = chunk_number
    val res = externals.clang_getCompletionChunkCompletionString(_completion_string, _chunk_number)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_getNumCompletionChunks(completion_string: CXCompletionString?): Int {
    val _completion_string = completion_string.rawValue
    val res = externals.clang_getNumCompletionChunks(_completion_string)
    return res
}

fun clang_getCompletionPriority(completion_string: CXCompletionString?): Int {
    val _completion_string = completion_string.rawValue
    val res = externals.clang_getCompletionPriority(_completion_string)
    return res
}

fun clang_getCompletionAvailability(completion_string: CXCompletionString?): CXAvailabilityKind {
    val _completion_string = completion_string.rawValue
    val res = externals.clang_getCompletionAvailability(_completion_string)
    return CXAvailabilityKind.byValue(res)
}

fun clang_getCompletionNumAnnotations(completion_string: CXCompletionString?): Int {
    val _completion_string = completion_string.rawValue
    val res = externals.clang_getCompletionNumAnnotations(_completion_string)
    return res
}

fun clang_getCompletionAnnotation(completion_string: CXCompletionString?, annotation_number: Int, retValPlacement: NativePlacement): CXString {
    val _completion_string = completion_string.rawValue
    val _annotation_number = annotation_number
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getCompletionAnnotation(_completion_string, _annotation_number, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getCompletionParent(completion_string: CXCompletionString?, kind: CPointer<CXCursorKind.Var>?, retValPlacement: NativePlacement): CXString {
    val _completion_string = completion_string.rawValue
    val _kind = kind.rawValue
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getCompletionParent(_completion_string, _kind, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getCompletionBriefComment(completion_string: CXCompletionString?, retValPlacement: NativePlacement): CXString {
    val _completion_string = completion_string.rawValue
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getCompletionBriefComment(_completion_string, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getCursorCompletionString(cursor: CXCursor): CXCompletionString? {
    val _cursor = cursor.rawPtr
    val res = externals.clang_getCursorCompletionString(_cursor)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_defaultCodeCompleteOptions(): Int {
    val res = externals.clang_defaultCodeCompleteOptions()
    return res
}

fun clang_codeCompleteAt(TU: CXTranslationUnit?, complete_filename: String?, complete_line: Int, complete_column: Int, unsaved_files: CPointer<CXUnsavedFile>?, num_unsaved_files: Int, options: Int): CPointer<CXCodeCompleteResults>? {
    return memScoped {
        val _TU = TU.rawValue
        val _complete_filename = complete_filename?.toCString(memScope).rawPtr
        val _complete_line = complete_line
        val _complete_column = complete_column
        val _unsaved_files = unsaved_files.rawValue
        val _num_unsaved_files = num_unsaved_files
        val _options = options
        val res = externals.clang_codeCompleteAt(_TU, _complete_filename, _complete_line, _complete_column, _unsaved_files, _num_unsaved_files, _options)
        CPointer.createNullable<CXCodeCompleteResults>(res)
    }
}

fun clang_sortCodeCompletionResults(Results: CPointer<CXCompletionResult>?, NumResults: Int): Unit {
    val _Results = Results.rawValue
    val _NumResults = NumResults
    val res = externals.clang_sortCodeCompletionResults(_Results, _NumResults)
    return res
}

fun clang_disposeCodeCompleteResults(Results: CPointer<CXCodeCompleteResults>?): Unit {
    val _Results = Results.rawValue
    val res = externals.clang_disposeCodeCompleteResults(_Results)
    return res
}

fun clang_codeCompleteGetNumDiagnostics(Results: CPointer<CXCodeCompleteResults>?): Int {
    val _Results = Results.rawValue
    val res = externals.clang_codeCompleteGetNumDiagnostics(_Results)
    return res
}

fun clang_codeCompleteGetDiagnostic(Results: CPointer<CXCodeCompleteResults>?, Index: Int): CXDiagnostic? {
    val _Results = Results.rawValue
    val _Index = Index
    val res = externals.clang_codeCompleteGetDiagnostic(_Results, _Index)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_codeCompleteGetContexts(Results: CPointer<CXCodeCompleteResults>?): Long {
    val _Results = Results.rawValue
    val res = externals.clang_codeCompleteGetContexts(_Results)
    return res
}

fun clang_codeCompleteGetContainerKind(Results: CPointer<CXCodeCompleteResults>?, IsIncomplete: CPointer<CInt32Var>?): CXCursorKind {
    val _Results = Results.rawValue
    val _IsIncomplete = IsIncomplete.rawValue
    val res = externals.clang_codeCompleteGetContainerKind(_Results, _IsIncomplete)
    return CXCursorKind.byValue(res)
}

fun clang_codeCompleteGetContainerUSR(Results: CPointer<CXCodeCompleteResults>?, retValPlacement: NativePlacement): CXString {
    val _Results = Results.rawValue
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_codeCompleteGetContainerUSR(_Results, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_codeCompleteGetObjCSelector(Results: CPointer<CXCodeCompleteResults>?, retValPlacement: NativePlacement): CXString {
    val _Results = Results.rawValue
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_codeCompleteGetObjCSelector(_Results, _retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_getClangVersion(retValPlacement: NativePlacement): CXString {
    val _retValPlacement = retValPlacement.alloc<CXString>().rawPtr
    val res = externals.clang_getClangVersion(_retValPlacement)
    return interpretPointed<CXString>(res)
}

fun clang_toggleCrashRecovery(isEnabled: Int): Unit {
    val _isEnabled = isEnabled
    val res = externals.clang_toggleCrashRecovery(_isEnabled)
    return res
}

fun clang_getInclusions(tu: CXTranslationUnit?, visitor: CXInclusionVisitor?, client_data: CXClientData?): Unit {
    val _tu = tu.rawValue
    val _visitor = visitor.rawValue
    val _client_data = client_data.rawValue
    val res = externals.clang_getInclusions(_tu, _visitor, _client_data)
    return res
}

fun clang_Cursor_Evaluate(C: CXCursor): CXEvalResult? {
    val _C = C.rawPtr
    val res = externals.clang_Cursor_Evaluate(_C)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_EvalResult_getKind(E: CXEvalResult?): CXEvalResultKind {
    val _E = E.rawValue
    val res = externals.clang_EvalResult_getKind(_E)
    return CXEvalResultKind.byValue(res)
}

fun clang_EvalResult_getAsInt(E: CXEvalResult?): Int {
    val _E = E.rawValue
    val res = externals.clang_EvalResult_getAsInt(_E)
    return res
}

fun clang_EvalResult_getAsDouble(E: CXEvalResult?): Double {
    val _E = E.rawValue
    val res = externals.clang_EvalResult_getAsDouble(_E)
    return res
}

fun clang_EvalResult_getAsStr(E: CXEvalResult?): CPointer<CInt8Var>? {
    val _E = E.rawValue
    val res = externals.clang_EvalResult_getAsStr(_E)
    return CPointer.createNullable<CInt8Var>(res)
}

fun clang_EvalResult_dispose(E: CXEvalResult?): Unit {
    val _E = E.rawValue
    val res = externals.clang_EvalResult_dispose(_E)
    return res
}

fun clang_getRemappings(path: String?): CXRemapping? {
    return memScoped {
        val _path = path?.toCString(memScope).rawPtr
        val res = externals.clang_getRemappings(_path)
        CPointer.createNullable<COpaque>(res)
    }
}

fun clang_getRemappingsFromFileList(filePaths: CPointer<CPointerVar<CInt8Var>>?, numFiles: Int): CXRemapping? {
    val _filePaths = filePaths.rawValue
    val _numFiles = numFiles
    val res = externals.clang_getRemappingsFromFileList(_filePaths, _numFiles)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_remap_getNumFiles(arg0: CXRemapping?): Int {
    val _arg0 = arg0.rawValue
    val res = externals.clang_remap_getNumFiles(_arg0)
    return res
}

fun clang_remap_getFilenames(arg0: CXRemapping?, index: Int, original: CPointer<CXString>?, transformed: CPointer<CXString>?): Unit {
    val _arg0 = arg0.rawValue
    val _index = index
    val _original = original.rawValue
    val _transformed = transformed.rawValue
    val res = externals.clang_remap_getFilenames(_arg0, _index, _original, _transformed)
    return res
}

fun clang_remap_dispose(arg0: CXRemapping?): Unit {
    val _arg0 = arg0.rawValue
    val res = externals.clang_remap_dispose(_arg0)
    return res
}

fun clang_findReferencesInFile(cursor: CXCursor, file: CXFile?, visitor: CXCursorAndRangeVisitor): CXResult {
    val _cursor = cursor.rawPtr
    val _file = file.rawValue
    val _visitor = visitor.rawPtr
    val res = externals.clang_findReferencesInFile(_cursor, _file, _visitor)
    return CXResult.byValue(res)
}

fun clang_findIncludesInFile(TU: CXTranslationUnit?, file: CXFile?, visitor: CXCursorAndRangeVisitor): CXResult {
    val _TU = TU.rawValue
    val _file = file.rawValue
    val _visitor = visitor.rawPtr
    val res = externals.clang_findIncludesInFile(_TU, _file, _visitor)
    return CXResult.byValue(res)
}

fun clang_index_isEntityObjCContainerKind(arg0: CXIdxEntityKind): Int {
    val _arg0 = arg0.value
    val res = externals.clang_index_isEntityObjCContainerKind(_arg0)
    return res
}

fun clang_index_getObjCContainerDeclInfo(arg0: CPointer<CXIdxDeclInfo>?): CPointer<CXIdxObjCContainerDeclInfo>? {
    val _arg0 = arg0.rawValue
    val res = externals.clang_index_getObjCContainerDeclInfo(_arg0)
    return CPointer.createNullable<CXIdxObjCContainerDeclInfo>(res)
}

fun clang_index_getObjCInterfaceDeclInfo(arg0: CPointer<CXIdxDeclInfo>?): CPointer<CXIdxObjCInterfaceDeclInfo>? {
    val _arg0 = arg0.rawValue
    val res = externals.clang_index_getObjCInterfaceDeclInfo(_arg0)
    return CPointer.createNullable<CXIdxObjCInterfaceDeclInfo>(res)
}

fun clang_index_getObjCCategoryDeclInfo(arg0: CPointer<CXIdxDeclInfo>?): CPointer<CXIdxObjCCategoryDeclInfo>? {
    val _arg0 = arg0.rawValue
    val res = externals.clang_index_getObjCCategoryDeclInfo(_arg0)
    return CPointer.createNullable<CXIdxObjCCategoryDeclInfo>(res)
}

fun clang_index_getObjCProtocolRefListInfo(arg0: CPointer<CXIdxDeclInfo>?): CPointer<CXIdxObjCProtocolRefListInfo>? {
    val _arg0 = arg0.rawValue
    val res = externals.clang_index_getObjCProtocolRefListInfo(_arg0)
    return CPointer.createNullable<CXIdxObjCProtocolRefListInfo>(res)
}

fun clang_index_getObjCPropertyDeclInfo(arg0: CPointer<CXIdxDeclInfo>?): CPointer<CXIdxObjCPropertyDeclInfo>? {
    val _arg0 = arg0.rawValue
    val res = externals.clang_index_getObjCPropertyDeclInfo(_arg0)
    return CPointer.createNullable<CXIdxObjCPropertyDeclInfo>(res)
}

fun clang_index_getIBOutletCollectionAttrInfo(arg0: CPointer<CXIdxAttrInfo>?): CPointer<CXIdxIBOutletCollectionAttrInfo>? {
    val _arg0 = arg0.rawValue
    val res = externals.clang_index_getIBOutletCollectionAttrInfo(_arg0)
    return CPointer.createNullable<CXIdxIBOutletCollectionAttrInfo>(res)
}

fun clang_index_getCXXClassDeclInfo(arg0: CPointer<CXIdxDeclInfo>?): CPointer<CXIdxCXXClassDeclInfo>? {
    val _arg0 = arg0.rawValue
    val res = externals.clang_index_getCXXClassDeclInfo(_arg0)
    return CPointer.createNullable<CXIdxCXXClassDeclInfo>(res)
}

fun clang_index_getClientContainer(arg0: CPointer<CXIdxContainerInfo>?): CXIdxClientContainer? {
    val _arg0 = arg0.rawValue
    val res = externals.clang_index_getClientContainer(_arg0)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_index_setClientContainer(arg0: CPointer<CXIdxContainerInfo>?, arg1: CXIdxClientContainer?): Unit {
    val _arg0 = arg0.rawValue
    val _arg1 = arg1.rawValue
    val res = externals.clang_index_setClientContainer(_arg0, _arg1)
    return res
}

fun clang_index_getClientEntity(arg0: CPointer<CXIdxEntityInfo>?): CXIdxClientEntity? {
    val _arg0 = arg0.rawValue
    val res = externals.clang_index_getClientEntity(_arg0)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_index_setClientEntity(arg0: CPointer<CXIdxEntityInfo>?, arg1: CXIdxClientEntity?): Unit {
    val _arg0 = arg0.rawValue
    val _arg1 = arg1.rawValue
    val res = externals.clang_index_setClientEntity(_arg0, _arg1)
    return res
}

fun clang_IndexAction_create(CIdx: CXIndex?): CXIndexAction? {
    val _CIdx = CIdx.rawValue
    val res = externals.clang_IndexAction_create(_CIdx)
    return CPointer.createNullable<COpaque>(res)
}

fun clang_IndexAction_dispose(arg0: CXIndexAction?): Unit {
    val _arg0 = arg0.rawValue
    val res = externals.clang_IndexAction_dispose(_arg0)
    return res
}

fun clang_indexSourceFile(arg0: CXIndexAction?, client_data: CXClientData?, index_callbacks: CPointer<IndexerCallbacks>?, index_callbacks_size: Int, index_options: Int, source_filename: String?, command_line_args: CPointer<CPointerVar<CInt8Var>>?, num_command_line_args: Int, unsaved_files: CPointer<CXUnsavedFile>?, num_unsaved_files: Int, out_TU: CPointer<CXTranslationUnitVar>?, TU_options: Int): Int {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _client_data = client_data.rawValue
        val _index_callbacks = index_callbacks.rawValue
        val _index_callbacks_size = index_callbacks_size
        val _index_options = index_options
        val _source_filename = source_filename?.toCString(memScope).rawPtr
        val _command_line_args = command_line_args.rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files.rawValue
        val _num_unsaved_files = num_unsaved_files
        val _out_TU = out_TU.rawValue
        val _TU_options = TU_options
        val res = externals.clang_indexSourceFile(_arg0, _client_data, _index_callbacks, _index_callbacks_size, _index_options, _source_filename, _command_line_args, _num_command_line_args, _unsaved_files, _num_unsaved_files, _out_TU, _TU_options)
        res
    }
}

fun clang_indexSourceFileFullArgv(arg0: CXIndexAction?, client_data: CXClientData?, index_callbacks: CPointer<IndexerCallbacks>?, index_callbacks_size: Int, index_options: Int, source_filename: String?, command_line_args: CPointer<CPointerVar<CInt8Var>>?, num_command_line_args: Int, unsaved_files: CPointer<CXUnsavedFile>?, num_unsaved_files: Int, out_TU: CPointer<CXTranslationUnitVar>?, TU_options: Int): Int {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _client_data = client_data.rawValue
        val _index_callbacks = index_callbacks.rawValue
        val _index_callbacks_size = index_callbacks_size
        val _index_options = index_options
        val _source_filename = source_filename?.toCString(memScope).rawPtr
        val _command_line_args = command_line_args.rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files.rawValue
        val _num_unsaved_files = num_unsaved_files
        val _out_TU = out_TU.rawValue
        val _TU_options = TU_options
        val res = externals.clang_indexSourceFileFullArgv(_arg0, _client_data, _index_callbacks, _index_callbacks_size, _index_options, _source_filename, _command_line_args, _num_command_line_args, _unsaved_files, _num_unsaved_files, _out_TU, _TU_options)
        res
    }
}

fun clang_indexTranslationUnit(arg0: CXIndexAction?, client_data: CXClientData?, index_callbacks: CPointer<IndexerCallbacks>?, index_callbacks_size: Int, index_options: Int, arg5: CXTranslationUnit?): Int {
    val _arg0 = arg0.rawValue
    val _client_data = client_data.rawValue
    val _index_callbacks = index_callbacks.rawValue
    val _index_callbacks_size = index_callbacks_size
    val _index_options = index_options
    val _arg5 = arg5.rawValue
    val res = externals.clang_indexTranslationUnit(_arg0, _client_data, _index_callbacks, _index_callbacks_size, _index_options, _arg5)
    return res
}

fun clang_indexLoc_getFileLocation(loc: CXIdxLoc, indexFile: CPointer<CXIdxClientFileVar>?, file: CPointer<CXFileVar>?, line: CPointer<CInt32Var>?, column: CPointer<CInt32Var>?, offset: CPointer<CInt32Var>?): Unit {
    val _loc = loc.rawPtr
    val _indexFile = indexFile.rawValue
    val _file = file.rawValue
    val _line = line.rawValue
    val _column = column.rawValue
    val _offset = offset.rawValue
    val res = externals.clang_indexLoc_getFileLocation(_loc, _indexFile, _file, _line, _column, _offset)
    return res
}

fun clang_indexLoc_getCXSourceLocation(loc: CXIdxLoc, retValPlacement: NativePlacement): CXSourceLocation {
    val _loc = loc.rawPtr
    val _retValPlacement = retValPlacement.alloc<CXSourceLocation>().rawPtr
    val res = externals.clang_indexLoc_getCXSourceLocation(_loc, _retValPlacement)
    return interpretPointed<CXSourceLocation>(res)
}

fun clang_Type_visitFields(T: CXType, visitor: CXFieldVisitor?, client_data: CXClientData?): Int {
    val _T = T.rawPtr
    val _visitor = visitor.rawValue
    val _client_data = client_data.rawValue
    val res = externals.clang_Type_visitFields(_T, _visitor, _client_data)
    return res
}

class __mbstate_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(128, 8)
    
    val __mbstate8: CArray<CInt8Var>
        get() = memberAt(0)
    
    val _mbstateL: CInt64Var
        get() = memberAt(0)
    
}

class __darwin_pthread_handler_rec(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    val __routine: CFunctionPointerVar<CFunctionType2>
        get() = memberAt(0)
    
    val __arg: COpaquePointerVar
        get() = memberAt(8)
    
    val __next: CPointerVar<__darwin_pthread_handler_rec>
        get() = memberAt(16)
    
}

class _opaque_pthread_attr_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(64, 8)
    
    val __sig: CInt64Var
        get() = memberAt(0)
    
    val __opaque: CArray<CInt8Var>
        get() = memberAt(8)
    
}

class _opaque_pthread_cond_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(48, 8)
    
    val __sig: CInt64Var
        get() = memberAt(0)
    
    val __opaque: CArray<CInt8Var>
        get() = memberAt(8)
    
}

class _opaque_pthread_condattr_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    val __sig: CInt64Var
        get() = memberAt(0)
    
    val __opaque: CArray<CInt8Var>
        get() = memberAt(8)
    
}

class _opaque_pthread_mutex_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(64, 8)
    
    val __sig: CInt64Var
        get() = memberAt(0)
    
    val __opaque: CArray<CInt8Var>
        get() = memberAt(8)
    
}

class _opaque_pthread_mutexattr_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    val __sig: CInt64Var
        get() = memberAt(0)
    
    val __opaque: CArray<CInt8Var>
        get() = memberAt(8)
    
}

class _opaque_pthread_once_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    val __sig: CInt64Var
        get() = memberAt(0)
    
    val __opaque: CArray<CInt8Var>
        get() = memberAt(8)
    
}

class _opaque_pthread_rwlock_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(200, 8)
    
    val __sig: CInt64Var
        get() = memberAt(0)
    
    val __opaque: CArray<CInt8Var>
        get() = memberAt(8)
    
}

class _opaque_pthread_rwlockattr_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    val __sig: CInt64Var
        get() = memberAt(0)
    
    val __opaque: CArray<CInt8Var>
        get() = memberAt(8)
    
}

class _opaque_pthread_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(8192, 8)
    
    val __sig: CInt64Var
        get() = memberAt(0)
    
    val __cleanup_stack: CPointerVar<__darwin_pthread_handler_rec>
        get() = memberAt(8)
    
    val __opaque: CArray<CInt8Var>
        get() = memberAt(16)
    
}

class timespec(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    val tv_sec: __darwin_time_tVar
        get() = memberAt(0)
    
    val tv_nsec: CInt64Var
        get() = memberAt(8)
    
}

class tm(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(56, 8)
    
    val tm_sec: CInt32Var
        get() = memberAt(0)
    
    val tm_min: CInt32Var
        get() = memberAt(4)
    
    val tm_hour: CInt32Var
        get() = memberAt(8)
    
    val tm_mday: CInt32Var
        get() = memberAt(12)
    
    val tm_mon: CInt32Var
        get() = memberAt(16)
    
    val tm_year: CInt32Var
        get() = memberAt(20)
    
    val tm_wday: CInt32Var
        get() = memberAt(24)
    
    val tm_yday: CInt32Var
        get() = memberAt(28)
    
    val tm_isdst: CInt32Var
        get() = memberAt(32)
    
    val tm_gmtoff: CInt64Var
        get() = memberAt(40)
    
    val tm_zone: CPointerVar<CInt8Var>
        get() = memberAt(48)
    
}

class CXString(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    val data: COpaquePointerVar
        get() = memberAt(0)
    
    val private_flags: CInt32Var
        get() = memberAt(8)
    
}

class CXStringSet(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    val Strings: CPointerVar<CXString>
        get() = memberAt(0)
    
    val Count: CInt32Var
        get() = memberAt(8)
    
}

class CXVirtualFileOverlayImpl(override val rawPtr: NativePtr) : COpaque

class CXModuleMapDescriptorImpl(override val rawPtr: NativePtr) : COpaque

class CXUnsavedFile(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    val Filename: CPointerVar<CInt8Var>
        get() = memberAt(0)
    
    val Contents: CPointerVar<CInt8Var>
        get() = memberAt(8)
    
    val Length: CInt64Var
        get() = memberAt(16)
    
}

class CXVersion(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(12, 4)
    
    val Major: CInt32Var
        get() = memberAt(0)
    
    val Minor: CInt32Var
        get() = memberAt(4)
    
    val Subminor: CInt32Var
        get() = memberAt(8)
    
}

class CXFileUniqueID(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    val data: CArray<CInt64Var>
        get() = memberAt(0)
    
}

class CXTranslationUnitImpl(override val rawPtr: NativePtr) : COpaque

class CXSourceLocation(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    val ptr_data: CArray<COpaquePointerVar>
        get() = memberAt(0)
    
    val int_data: CInt32Var
        get() = memberAt(16)
    
}

class CXSourceRange(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    val ptr_data: CArray<COpaquePointerVar>
        get() = memberAt(0)
    
    val begin_int_data: CInt32Var
        get() = memberAt(16)
    
    val end_int_data: CInt32Var
        get() = memberAt(20)
    
}

class CXSourceRangeList(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    val count: CInt32Var
        get() = memberAt(0)
    
    val ranges: CPointerVar<CXSourceRange>
        get() = memberAt(8)
    
}

class CXTUResourceUsageEntry(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    val kind: CXTUResourceUsageKind.Var
        get() = memberAt(0)
    
    val amount: CInt64Var
        get() = memberAt(8)
    
}

class CXTUResourceUsage(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    val data: COpaquePointerVar
        get() = memberAt(0)
    
    val numEntries: CInt32Var
        get() = memberAt(8)
    
    val entries: CPointerVar<CXTUResourceUsageEntry>
        get() = memberAt(16)
    
}

class CXCursor(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(32, 8)
    
    val kind: CXCursorKind.Var
        get() = memberAt(0)
    
    val xdata: CInt32Var
        get() = memberAt(4)
    
    val data: CArray<COpaquePointerVar>
        get() = memberAt(8)
    
}

class CXPlatformAvailability(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(72, 8)
    
    val Platform: CXString
        get() = memberAt(0)
    
    val Introduced: CXVersion
        get() = memberAt(16)
    
    val Deprecated: CXVersion
        get() = memberAt(28)
    
    val Obsoleted: CXVersion
        get() = memberAt(40)
    
    val Unavailable: CInt32Var
        get() = memberAt(52)
    
    val Message: CXString
        get() = memberAt(56)
    
}

class CXCursorSetImpl(override val rawPtr: NativePtr) : COpaque

class CXType(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    val kind: CXTypeKind.Var
        get() = memberAt(0)
    
    val data: CArray<COpaquePointerVar>
        get() = memberAt(8)
    
}

class CXToken(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    val int_data: CArray<CInt32Var>
        get() = memberAt(0)
    
    val ptr_data: COpaquePointerVar
        get() = memberAt(16)
    
}

class CXCompletionResult(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    val CursorKind: CXCursorKind.Var
        get() = memberAt(0)
    
    val CompletionString: CXCompletionStringVar
        get() = memberAt(8)
    
}

class CXCodeCompleteResults(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    val Results: CPointerVar<CXCompletionResult>
        get() = memberAt(0)
    
    val NumResults: CInt32Var
        get() = memberAt(8)
    
}

class CXCursorAndRangeVisitor(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    val context: COpaquePointerVar
        get() = memberAt(0)
    
    val visit: CFunctionPointerVar<CFunctionType5>
        get() = memberAt(8)
    
}

class CXIdxLoc(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    val ptr_data: CArray<COpaquePointerVar>
        get() = memberAt(0)
    
    val int_data: CInt32Var
        get() = memberAt(16)
    
}

class CXIdxIncludedFileInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(56, 8)
    
    val hashLoc: CXIdxLoc
        get() = memberAt(0)
    
    val filename: CPointerVar<CInt8Var>
        get() = memberAt(24)
    
    val file: CXFileVar
        get() = memberAt(32)
    
    val isImport: CInt32Var
        get() = memberAt(40)
    
    val isAngled: CInt32Var
        get() = memberAt(44)
    
    val isModuleImport: CInt32Var
        get() = memberAt(48)
    
}

class CXIdxImportedASTFileInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(48, 8)
    
    val file: CXFileVar
        get() = memberAt(0)
    
    val module: CXModuleVar
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(16)
    
    val isImplicit: CInt32Var
        get() = memberAt(40)
    
}

class CXIdxAttrInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(64, 8)
    
    val kind: CXIdxAttrKind.Var
        get() = memberAt(0)
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
    
}

class CXIdxEntityInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(80, 8)
    
    val kind: CXIdxEntityKind.Var
        get() = memberAt(0)
    
    val templateKind: CXIdxEntityCXXTemplateKind.Var
        get() = memberAt(4)
    
    val lang: CXIdxEntityLanguage.Var
        get() = memberAt(8)
    
    val name: CPointerVar<CInt8Var>
        get() = memberAt(16)
    
    val USR: CPointerVar<CInt8Var>
        get() = memberAt(24)
    
    val cursor: CXCursor
        get() = memberAt(32)
    
    val attributes: CPointerVar<CPointerVar<CXIdxAttrInfo>>
        get() = memberAt(64)
    
    val numAttributes: CInt32Var
        get() = memberAt(72)
    
}

class CXIdxContainerInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(32, 8)
    
    val cursor: CXCursor
        get() = memberAt(0)
    
}

class CXIdxIBOutletCollectionAttrInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(72, 8)
    
    val attrInfo: CPointerVar<CXIdxAttrInfo>
        get() = memberAt(0)
    
    val objcClass: CPointerVar<CXIdxEntityInfo>
        get() = memberAt(8)
    
    val classCursor: CXCursor
        get() = memberAt(16)
    
    val classLoc: CXIdxLoc
        get() = memberAt(48)
    
}

class CXIdxDeclInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(128, 8)
    
    val entityInfo: CPointerVar<CXIdxEntityInfo>
        get() = memberAt(0)
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
    
    val semanticContainer: CPointerVar<CXIdxContainerInfo>
        get() = memberAt(64)
    
    val lexicalContainer: CPointerVar<CXIdxContainerInfo>
        get() = memberAt(72)
    
    val isRedeclaration: CInt32Var
        get() = memberAt(80)
    
    val isDefinition: CInt32Var
        get() = memberAt(84)
    
    val isContainer: CInt32Var
        get() = memberAt(88)
    
    val declAsContainer: CPointerVar<CXIdxContainerInfo>
        get() = memberAt(96)
    
    val isImplicit: CInt32Var
        get() = memberAt(104)
    
    val attributes: CPointerVar<CPointerVar<CXIdxAttrInfo>>
        get() = memberAt(112)
    
    val numAttributes: CInt32Var
        get() = memberAt(120)
    
    val flags: CInt32Var
        get() = memberAt(124)
    
}

class CXIdxObjCContainerDeclInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    val declInfo: CPointerVar<CXIdxDeclInfo>
        get() = memberAt(0)
    
    val kind: CXIdxObjCContainerKind.Var
        get() = memberAt(8)
    
}

class CXIdxBaseClassInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(64, 8)
    
    val base: CPointerVar<CXIdxEntityInfo>
        get() = memberAt(0)
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
    
}

class CXIdxObjCProtocolRefInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(64, 8)
    
    val protocol: CPointerVar<CXIdxEntityInfo>
        get() = memberAt(0)
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
    
}

class CXIdxObjCProtocolRefListInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    val protocols: CPointerVar<CPointerVar<CXIdxObjCProtocolRefInfo>>
        get() = memberAt(0)
    
    val numProtocols: CInt32Var
        get() = memberAt(8)
    
}

class CXIdxObjCInterfaceDeclInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    val containerInfo: CPointerVar<CXIdxObjCContainerDeclInfo>
        get() = memberAt(0)
    
    val superInfo: CPointerVar<CXIdxBaseClassInfo>
        get() = memberAt(8)
    
    val protocols: CPointerVar<CXIdxObjCProtocolRefListInfo>
        get() = memberAt(16)
    
}

class CXIdxObjCCategoryDeclInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(80, 8)
    
    val containerInfo: CPointerVar<CXIdxObjCContainerDeclInfo>
        get() = memberAt(0)
    
    val objcClass: CPointerVar<CXIdxEntityInfo>
        get() = memberAt(8)
    
    val classCursor: CXCursor
        get() = memberAt(16)
    
    val classLoc: CXIdxLoc
        get() = memberAt(48)
    
    val protocols: CPointerVar<CXIdxObjCProtocolRefListInfo>
        get() = memberAt(72)
    
}

class CXIdxObjCPropertyDeclInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    val declInfo: CPointerVar<CXIdxDeclInfo>
        get() = memberAt(0)
    
    val getter: CPointerVar<CXIdxEntityInfo>
        get() = memberAt(8)
    
    val setter: CPointerVar<CXIdxEntityInfo>
        get() = memberAt(16)
    
}

class CXIdxCXXClassDeclInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    val declInfo: CPointerVar<CXIdxDeclInfo>
        get() = memberAt(0)
    
    val bases: CPointerVar<CPointerVar<CXIdxBaseClassInfo>>
        get() = memberAt(8)
    
    val numBases: CInt32Var
        get() = memberAt(16)
    
}

class CXIdxEntityRefInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(88, 8)
    
    val kind: CXIdxEntityRefKind.Var
        get() = memberAt(0)
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
    
    val referencedEntity: CPointerVar<CXIdxEntityInfo>
        get() = memberAt(64)
    
    val parentEntity: CPointerVar<CXIdxEntityInfo>
        get() = memberAt(72)
    
    val container: CPointerVar<CXIdxContainerInfo>
        get() = memberAt(80)
    
}

class IndexerCallbacks(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(64, 8)
    
    val abortQuery: CFunctionPointerVar<CFunctionType6>
        get() = memberAt(0)
    
    val diagnostic: CFunctionPointerVar<CFunctionType7>
        get() = memberAt(8)
    
    val enteredMainFile: CFunctionPointerVar<CFunctionType8>
        get() = memberAt(16)
    
    val ppIncludedFile: CFunctionPointerVar<CFunctionType9>
        get() = memberAt(24)
    
    val importedASTFile: CFunctionPointerVar<CFunctionType10>
        get() = memberAt(32)
    
    val startedTranslationUnit: CFunctionPointerVar<CFunctionType11>
        get() = memberAt(40)
    
    val indexDeclaration: CFunctionPointerVar<CFunctionType12>
        get() = memberAt(48)
    
    val indexEntityReference: CFunctionPointerVar<CFunctionType13>
        get() = memberAt(56)
    
}

enum class clockid_t(val value: Int) {
    _CLOCK_REALTIME(0),
    _CLOCK_MONOTONIC(6),
    _CLOCK_MONOTONIC_RAW(4),
    _CLOCK_MONOTONIC_RAW_APPROX(5),
    _CLOCK_UPTIME_RAW(8),
    _CLOCK_UPTIME_RAW_APPROX(9),
    _CLOCK_PROCESS_CPUTIME_ID(12),
    _CLOCK_THREAD_CPUTIME_ID(16),
    ;
    
    companion object {
        fun byValue(value: Int) = clockid_t.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: clockid_t
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
    }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXErrorCode
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXAvailabilityKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXGlobalOptFlags
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXDiagnosticSeverity
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXLoadDiag_Error
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXDiagnosticDisplayOptions
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    CXTranslationUnit_KeepGoing(512),
    ;
    
    companion object {
        fun byValue(value: Int) = CXTranslationUnit_Flags.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXTranslationUnit_Flags
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
    }
}

enum class CXSaveTranslationUnit_Flags(val value: Int) {
    CXSaveTranslationUnit_None(0),
    ;
    
    companion object {
        fun byValue(value: Int) = CXSaveTranslationUnit_Flags.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXSaveTranslationUnit_Flags
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXSaveError
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
    }
}

enum class CXReparse_Flags(val value: Int) {
    CXReparse_None(0),
    ;
    
    companion object {
        fun byValue(value: Int) = CXReparse_Flags.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXReparse_Flags
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXTUResourceUsageKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    CXCursor_ObjCAvailabilityCheckExpr(148),
    CXCursor_LastExpr(148),
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
    CXCursor_OMPTargetEnterDataDirective(261),
    CXCursor_OMPTargetExitDataDirective(262),
    CXCursor_OMPTargetParallelDirective(263),
    CXCursor_OMPTargetParallelForDirective(264),
    CXCursor_OMPTargetUpdateDirective(265),
    CXCursor_OMPDistributeParallelForDirective(266),
    CXCursor_OMPDistributeParallelForSimdDirective(267),
    CXCursor_OMPDistributeSimdDirective(268),
    CXCursor_OMPTargetParallelForSimdDirective(269),
    CXCursor_LastStmt(269),
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
    CXCursor_StaticAssert(602),
    CXCursor_FirstExtraDecl(600),
    CXCursor_LastExtraDecl(602),
    CXCursor_OverloadCandidate(700),
    ;
    
    companion object {
        fun byValue(value: Int) = CXCursorKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXCursorKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXLinkageKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXVisibilityKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXLanguageKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    CXType_Float128(30),
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
    CXType_Elaborated(119),
    ;
    
    companion object {
        fun byValue(value: Int) = CXTypeKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXTypeKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    CXCallingConv_Swift(13),
    CXCallingConv_PreserveMost(14),
    CXCallingConv_PreserveAll(15),
    CXCallingConv_Invalid(100),
    CXCallingConv_Unexposed(200),
    ;
    
    companion object {
        fun byValue(value: Int) = CXCallingConv.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXCallingConv
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXTemplateArgumentKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXTypeLayoutError
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXRefQualifierKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CX_CXXAccessSpecifier
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CX_StorageClass
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXChildVisitResult
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    CXObjCPropertyAttr_class(4096),
    ;
    
    companion object {
        fun byValue(value: Int) = CXObjCPropertyAttrKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXObjCPropertyAttrKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXObjCDeclQualifierKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXNameRefFlags
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXTokenKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXCompletionChunkKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXCodeComplete_Flags
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXCompletionContext
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
    }
}

enum class CXEvalResultKind(val value: Int) {
    CXEval_Int(1),
    CXEval_Float(2),
    CXEval_ObjCStrLiteral(3),
    CXEval_StrLiteral(4),
    CXEval_CFStr(5),
    CXEval_Other(6),
    CXEval_UnExposed(0),
    ;
    
    companion object {
        fun byValue(value: Int) = CXEvalResultKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXEvalResultKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
    }
}

enum class CXVisitorResult(val value: Int) {
    CXVisit_Break(0),
    CXVisit_Continue(1),
    ;
    
    companion object {
        fun byValue(value: Int) = CXVisitorResult.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXVisitorResult
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXResult
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXIdxEntityKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXIdxEntityLanguage
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXIdxEntityCXXTemplateKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXIdxAttrKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
    }
}

enum class CXIdxDeclInfoFlags(val value: Int) {
    CXIdxDeclFlag_Skipped(1),
    ;
    
    companion object {
        fun byValue(value: Int) = CXIdxDeclInfoFlags.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXIdxDeclInfoFlags
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXIdxObjCContainerKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
    }
}

enum class CXIdxEntityRefKind(val value: Int) {
    CXIdxEntityRef_Direct(1),
    CXIdxEntityRef_Implicit(2),
    ;
    
    companion object {
        fun byValue(value: Int) = CXIdxEntityRefKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXIdxEntityRefKind
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
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
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(CInt32Var.size.toInt())
        var value: CXIndexOptFlags
            get() = byValue(this.reinterpret<CInt32Var>().value)
            set(value) { this.reinterpret<CInt32Var>().value = value.value }
    }
}

typealias __darwin_time_tVar = CInt64VarWithValueMappedTo<__darwin_time_t>
typealias __darwin_time_t = Long

typealias __darwin_clock_tVar = CInt64VarWithValueMappedTo<__darwin_clock_t>
typealias __darwin_clock_t = Long

typealias clock_tVar = CInt64VarWithValueMappedTo<clock_t>
typealias clock_t = __darwin_clock_t

typealias time_tVar = CInt64VarWithValueMappedTo<time_t>
typealias time_t = __darwin_time_t

typealias __darwin_size_tVar = CInt64VarWithValueMappedTo<__darwin_size_t>
typealias __darwin_size_t = Long

typealias size_tVar = CInt64VarWithValueMappedTo<size_t>
typealias size_t = __darwin_size_t

typealias __uint64_tVar = CInt64VarWithValueMappedTo<__uint64_t>
typealias __uint64_t = Long

typealias CXVirtualFileOverlayVar = CPointerVarWithValueMappedTo<CXVirtualFileOverlay>
typealias CXVirtualFileOverlay = CPointer<CXVirtualFileOverlayImpl>

typealias CXModuleMapDescriptorVar = CPointerVarWithValueMappedTo<CXModuleMapDescriptor>
typealias CXModuleMapDescriptor = CPointer<CXModuleMapDescriptorImpl>

typealias CXIndexVar = CPointerVarWithValueMappedTo<CXIndex>
typealias CXIndex = COpaquePointer

typealias CXFileVar = CPointerVarWithValueMappedTo<CXFile>
typealias CXFile = COpaquePointer

typealias CXTranslationUnitVar = CPointerVarWithValueMappedTo<CXTranslationUnit>
typealias CXTranslationUnit = CPointer<CXTranslationUnitImpl>

typealias CXDiagnosticSetVar = CPointerVarWithValueMappedTo<CXDiagnosticSet>
typealias CXDiagnosticSet = COpaquePointer

typealias CXDiagnosticVar = CPointerVarWithValueMappedTo<CXDiagnostic>
typealias CXDiagnostic = COpaquePointer

typealias CXCursorSetVar = CPointerVarWithValueMappedTo<CXCursorSet>
typealias CXCursorSet = CPointer<CXCursorSetImpl>

typealias CXCursorVisitorVar = CPointerVarWithValueMappedTo<CXCursorVisitor>
typealias CXCursorVisitor = CFunctionPointer<CFunctionType1>

typealias CXClientDataVar = CPointerVarWithValueMappedTo<CXClientData>
typealias CXClientData = COpaquePointer

typealias CXModuleVar = CPointerVarWithValueMappedTo<CXModule>
typealias CXModule = COpaquePointer

typealias CXCompletionStringVar = CPointerVarWithValueMappedTo<CXCompletionString>
typealias CXCompletionString = COpaquePointer

typealias CXInclusionVisitorVar = CPointerVarWithValueMappedTo<CXInclusionVisitor>
typealias CXInclusionVisitor = CFunctionPointer<CFunctionType3>

typealias CXEvalResultVar = CPointerVarWithValueMappedTo<CXEvalResult>
typealias CXEvalResult = COpaquePointer

typealias CXRemappingVar = CPointerVarWithValueMappedTo<CXRemapping>
typealias CXRemapping = COpaquePointer

typealias CXIdxClientContainerVar = CPointerVarWithValueMappedTo<CXIdxClientContainer>
typealias CXIdxClientContainer = COpaquePointer

typealias CXIdxClientEntityVar = CPointerVarWithValueMappedTo<CXIdxClientEntity>
typealias CXIdxClientEntity = COpaquePointer

typealias CXIndexActionVar = CPointerVarWithValueMappedTo<CXIndexAction>
typealias CXIndexAction = COpaquePointer

typealias CXIdxClientFileVar = CPointerVarWithValueMappedTo<CXIdxClientFile>
typealias CXIdxClientFile = COpaquePointer

typealias CXFieldVisitorVar = CPointerVarWithValueMappedTo<CXFieldVisitor>
typealias CXFieldVisitor = CFunctionPointer<CFunctionType4>

object CFunctionType1 : CAdaptedFunctionTypeImpl<(CXCursor, CXCursor, COpaquePointer?) -> CXChildVisitResult>(UInt32, Struct(UInt32, SInt32, Struct(Pointer, Pointer, Pointer)), Struct(UInt32, SInt32, Struct(Pointer, Pointer, Pointer)), Pointer) {
    override fun invoke(function: (CXCursor, CXCursor, COpaquePointer?) -> CXChildVisitResult,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<CXCursor>().pointed, args[1].value!!.reinterpret<CXCursor>().pointed, args[2].value!!.reinterpret<COpaquePointerVar>().pointed.value)
        ret.reinterpret<CXChildVisitResult.Var>().pointed.value = res
    }
}

object CFunctionType2 : CAdaptedFunctionTypeImpl<(COpaquePointer?) -> Unit>(Void, Pointer) {
    override fun invoke(function: (COpaquePointer?) -> Unit,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value)
    }
}

object CFunctionType3 : CAdaptedFunctionTypeImpl<(COpaquePointer?, CPointer<CXSourceLocation>?, Int, COpaquePointer?) -> Unit>(Void, Pointer, Pointer, UInt32, Pointer) {
    override fun invoke(function: (COpaquePointer?, CPointer<CXSourceLocation>?, Int, COpaquePointer?) -> Unit,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<CPointerVar<CXSourceLocation>>().pointed.value, args[2].value!!.reinterpret<CInt32Var>().pointed.value, args[3].value!!.reinterpret<COpaquePointerVar>().pointed.value)
    }
}

object CFunctionType4 : CAdaptedFunctionTypeImpl<(CXCursor, COpaquePointer?) -> CXVisitorResult>(UInt32, Struct(UInt32, SInt32, Struct(Pointer, Pointer, Pointer)), Pointer) {
    override fun invoke(function: (CXCursor, COpaquePointer?) -> CXVisitorResult,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<CXCursor>().pointed, args[1].value!!.reinterpret<COpaquePointerVar>().pointed.value)
        ret.reinterpret<CXVisitorResult.Var>().pointed.value = res
    }
}

object CFunctionType5 : CAdaptedFunctionTypeImpl<(COpaquePointer?, CXCursor, CXSourceRange) -> CXVisitorResult>(UInt32, Pointer, Struct(UInt32, SInt32, Struct(Pointer, Pointer, Pointer)), Struct(Struct(Pointer, Pointer), UInt32, UInt32)) {
    override fun invoke(function: (COpaquePointer?, CXCursor, CXSourceRange) -> CXVisitorResult,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<CXCursor>().pointed, args[2].value!!.reinterpret<CXSourceRange>().pointed)
        ret.reinterpret<CXVisitorResult.Var>().pointed.value = res
    }
}

object CFunctionType6 : CAdaptedFunctionTypeImpl<(COpaquePointer?, COpaquePointer?) -> Int>(SInt32, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, COpaquePointer?) -> Int,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<COpaquePointerVar>().pointed.value)
        ret.reinterpret<CInt32Var>().pointed.value = res
    }
}

object CFunctionType7 : CAdaptedFunctionTypeImpl<(COpaquePointer?, COpaquePointer?, COpaquePointer?) -> Unit>(Void, Pointer, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, COpaquePointer?, COpaquePointer?) -> Unit,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[2].value!!.reinterpret<COpaquePointerVar>().pointed.value)
    }
}

object CFunctionType8 : CAdaptedFunctionTypeImpl<(COpaquePointer?, COpaquePointer?, COpaquePointer?) -> COpaquePointer?>(Pointer, Pointer, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, COpaquePointer?, COpaquePointer?) -> COpaquePointer?,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[2].value!!.reinterpret<COpaquePointerVar>().pointed.value)
        ret.reinterpret<COpaquePointerVar>().pointed.value = res
    }
}

object CFunctionType9 : CAdaptedFunctionTypeImpl<(COpaquePointer?, CPointer<CXIdxIncludedFileInfo>?) -> COpaquePointer?>(Pointer, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, CPointer<CXIdxIncludedFileInfo>?) -> COpaquePointer?,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<CPointerVar<CXIdxIncludedFileInfo>>().pointed.value)
        ret.reinterpret<COpaquePointerVar>().pointed.value = res
    }
}

object CFunctionType10 : CAdaptedFunctionTypeImpl<(COpaquePointer?, CPointer<CXIdxImportedASTFileInfo>?) -> COpaquePointer?>(Pointer, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, CPointer<CXIdxImportedASTFileInfo>?) -> COpaquePointer?,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<CPointerVar<CXIdxImportedASTFileInfo>>().pointed.value)
        ret.reinterpret<COpaquePointerVar>().pointed.value = res
    }
}

object CFunctionType11 : CAdaptedFunctionTypeImpl<(COpaquePointer?, COpaquePointer?) -> COpaquePointer?>(Pointer, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, COpaquePointer?) -> COpaquePointer?,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<COpaquePointerVar>().pointed.value)
        ret.reinterpret<COpaquePointerVar>().pointed.value = res
    }
}

object CFunctionType12 : CAdaptedFunctionTypeImpl<(COpaquePointer?, CPointer<CXIdxDeclInfo>?) -> Unit>(Void, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, CPointer<CXIdxDeclInfo>?) -> Unit,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<CPointerVar<CXIdxDeclInfo>>().pointed.value)
    }
}

object CFunctionType13 : CAdaptedFunctionTypeImpl<(COpaquePointer?, CPointer<CXIdxEntityRefInfo>?) -> Unit>(Void, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, CPointer<CXIdxEntityRefInfo>?) -> Unit,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<CPointerVar<CXIdxEntityRefInfo>>().pointed.value)
    }
}

object externals {
    init { System.loadLibrary("clangstubs") }
    external fun asctime(arg0: NativePtr): NativePtr
    
    external fun clock(): Long
    
    external fun ctime(arg0: NativePtr): NativePtr
    
    external fun difftime(arg0: Long, arg1: Long): Double
    
    external fun getdate(arg0: NativePtr): NativePtr
    
    external fun gmtime(arg0: NativePtr): NativePtr
    
    external fun localtime(arg0: NativePtr): NativePtr
    
    external fun mktime(arg0: NativePtr): Long
    
    external fun strftime(arg0: NativePtr, arg1: Long, arg2: NativePtr, arg3: NativePtr): Long
    
    external fun strptime(arg0: NativePtr, arg1: NativePtr, arg2: NativePtr): NativePtr
    
    external fun time(arg0: NativePtr): Long
    
    external fun tzset(): Unit
    
    external fun asctime_r(arg0: NativePtr, arg1: NativePtr): NativePtr
    
    external fun ctime_r(arg0: NativePtr, arg1: NativePtr): NativePtr
    
    external fun gmtime_r(arg0: NativePtr, arg1: NativePtr): NativePtr
    
    external fun localtime_r(arg0: NativePtr, arg1: NativePtr): NativePtr
    
    external fun posix2time(arg0: Long): Long
    
    external fun tzsetwall(): Unit
    
    external fun time2posix(arg0: Long): Long
    
    external fun timelocal(arg0: NativePtr): Long
    
    external fun timegm(arg0: NativePtr): Long
    
    external fun nanosleep(__rqtp: NativePtr, __rmtp: NativePtr): Int
    
    external fun clock_getres(__clock_id: Int, __res: NativePtr): Int
    
    external fun clock_gettime(__clock_id: Int, __tp: NativePtr): Int
    
    external fun clock_gettime_nsec_np(__clock_id: Int): Long
    
    external fun clock_settime(__clock_id: Int, __tp: NativePtr): Int
    
    external fun clang_getCString(string: NativePtr): NativePtr
    
    external fun clang_disposeString(string: NativePtr): Unit
    
    external fun clang_disposeStringSet(set: NativePtr): Unit
    
    external fun clang_getBuildSessionTimestamp(): Long
    
    external fun clang_VirtualFileOverlay_create(options: Int): NativePtr
    
    external fun clang_VirtualFileOverlay_addFileMapping(arg0: NativePtr, virtualPath: NativePtr, realPath: NativePtr): Int
    
    external fun clang_VirtualFileOverlay_setCaseSensitivity(arg0: NativePtr, caseSensitive: Int): Int
    
    external fun clang_VirtualFileOverlay_writeToBuffer(arg0: NativePtr, options: Int, out_buffer_ptr: NativePtr, out_buffer_size: NativePtr): Int
    
    external fun clang_free(buffer: NativePtr): Unit
    
    external fun clang_VirtualFileOverlay_dispose(arg0: NativePtr): Unit
    
    external fun clang_ModuleMapDescriptor_create(options: Int): NativePtr
    
    external fun clang_ModuleMapDescriptor_setFrameworkModuleName(arg0: NativePtr, name: NativePtr): Int
    
    external fun clang_ModuleMapDescriptor_setUmbrellaHeader(arg0: NativePtr, name: NativePtr): Int
    
    external fun clang_ModuleMapDescriptor_writeToBuffer(arg0: NativePtr, options: Int, out_buffer_ptr: NativePtr, out_buffer_size: NativePtr): Int
    
    external fun clang_ModuleMapDescriptor_dispose(arg0: NativePtr): Unit
    
    external fun clang_createIndex(excludeDeclarationsFromPCH: Int, displayDiagnostics: Int): NativePtr
    
    external fun clang_disposeIndex(index: NativePtr): Unit
    
    external fun clang_CXIndex_setGlobalOptions(arg0: NativePtr, options: Int): Unit
    
    external fun clang_CXIndex_getGlobalOptions(arg0: NativePtr): Int
    
    external fun clang_getFileName(SFile: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getFileTime(SFile: NativePtr): Long
    
    external fun clang_getFileUniqueID(file: NativePtr, outID: NativePtr): Int
    
    external fun clang_isFileMultipleIncludeGuarded(tu: NativePtr, file: NativePtr): Int
    
    external fun clang_getFile(tu: NativePtr, file_name: NativePtr): NativePtr
    
    external fun clang_File_isEqual(file1: NativePtr, file2: NativePtr): Int
    
    external fun clang_getNullLocation(retValPlacement: NativePtr): NativePtr
    
    external fun clang_equalLocations(loc1: NativePtr, loc2: NativePtr): Int
    
    external fun clang_getLocation(tu: NativePtr, file: NativePtr, line: Int, column: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getLocationForOffset(tu: NativePtr, file: NativePtr, offset: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Location_isInSystemHeader(location: NativePtr): Int
    
    external fun clang_Location_isFromMainFile(location: NativePtr): Int
    
    external fun clang_getNullRange(retValPlacement: NativePtr): NativePtr
    
    external fun clang_getRange(begin: NativePtr, end: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_equalRanges(range1: NativePtr, range2: NativePtr): Int
    
    external fun clang_Range_isNull(range: NativePtr): Int
    
    external fun clang_getExpansionLocation(location: NativePtr, file: NativePtr, line: NativePtr, column: NativePtr, offset: NativePtr): Unit
    
    external fun clang_getPresumedLocation(location: NativePtr, filename: NativePtr, line: NativePtr, column: NativePtr): Unit
    
    external fun clang_getInstantiationLocation(location: NativePtr, file: NativePtr, line: NativePtr, column: NativePtr, offset: NativePtr): Unit
    
    external fun clang_getSpellingLocation(location: NativePtr, file: NativePtr, line: NativePtr, column: NativePtr, offset: NativePtr): Unit
    
    external fun clang_getFileLocation(location: NativePtr, file: NativePtr, line: NativePtr, column: NativePtr, offset: NativePtr): Unit
    
    external fun clang_getRangeStart(range: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getRangeEnd(range: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getSkippedRanges(tu: NativePtr, file: NativePtr): NativePtr
    
    external fun clang_disposeSourceRangeList(ranges: NativePtr): Unit
    
    external fun clang_getNumDiagnosticsInSet(Diags: NativePtr): Int
    
    external fun clang_getDiagnosticInSet(Diags: NativePtr, Index: Int): NativePtr
    
    external fun clang_loadDiagnostics(file: NativePtr, error: NativePtr, errorString: NativePtr): NativePtr
    
    external fun clang_disposeDiagnosticSet(Diags: NativePtr): Unit
    
    external fun clang_getChildDiagnostics(D: NativePtr): NativePtr
    
    external fun clang_getNumDiagnostics(Unit: NativePtr): Int
    
    external fun clang_getDiagnostic(Unit: NativePtr, Index: Int): NativePtr
    
    external fun clang_getDiagnosticSetFromTU(Unit: NativePtr): NativePtr
    
    external fun clang_disposeDiagnostic(Diagnostic: NativePtr): Unit
    
    external fun clang_formatDiagnostic(Diagnostic: NativePtr, Options: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_defaultDiagnosticDisplayOptions(): Int
    
    external fun clang_getDiagnosticSeverity(arg0: NativePtr): Int
    
    external fun clang_getDiagnosticLocation(arg0: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getDiagnosticSpelling(arg0: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getDiagnosticOption(Diag: NativePtr, Disable: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getDiagnosticCategory(arg0: NativePtr): Int
    
    external fun clang_getDiagnosticCategoryName(Category: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getDiagnosticCategoryText(arg0: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getDiagnosticNumRanges(arg0: NativePtr): Int
    
    external fun clang_getDiagnosticRange(Diagnostic: NativePtr, Range: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getDiagnosticNumFixIts(Diagnostic: NativePtr): Int
    
    external fun clang_getDiagnosticFixIt(Diagnostic: NativePtr, FixIt: Int, ReplacementRange: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getTranslationUnitSpelling(CTUnit: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_createTranslationUnitFromSourceFile(CIdx: NativePtr, source_filename: NativePtr, num_clang_command_line_args: Int, clang_command_line_args: NativePtr, num_unsaved_files: Int, unsaved_files: NativePtr): NativePtr
    
    external fun clang_createTranslationUnit(CIdx: NativePtr, ast_filename: NativePtr): NativePtr
    
    external fun clang_createTranslationUnit2(CIdx: NativePtr, ast_filename: NativePtr, out_TU: NativePtr): Int
    
    external fun clang_defaultEditingTranslationUnitOptions(): Int
    
    external fun clang_parseTranslationUnit(CIdx: NativePtr, source_filename: NativePtr, command_line_args: NativePtr, num_command_line_args: Int, unsaved_files: NativePtr, num_unsaved_files: Int, options: Int): NativePtr
    
    external fun clang_parseTranslationUnit2(CIdx: NativePtr, source_filename: NativePtr, command_line_args: NativePtr, num_command_line_args: Int, unsaved_files: NativePtr, num_unsaved_files: Int, options: Int, out_TU: NativePtr): Int
    
    external fun clang_parseTranslationUnit2FullArgv(CIdx: NativePtr, source_filename: NativePtr, command_line_args: NativePtr, num_command_line_args: Int, unsaved_files: NativePtr, num_unsaved_files: Int, options: Int, out_TU: NativePtr): Int
    
    external fun clang_defaultSaveOptions(TU: NativePtr): Int
    
    external fun clang_saveTranslationUnit(TU: NativePtr, FileName: NativePtr, options: Int): Int
    
    external fun clang_disposeTranslationUnit(arg0: NativePtr): Unit
    
    external fun clang_defaultReparseOptions(TU: NativePtr): Int
    
    external fun clang_reparseTranslationUnit(TU: NativePtr, num_unsaved_files: Int, unsaved_files: NativePtr, options: Int): Int
    
    external fun clang_getTUResourceUsageName(kind: Int): NativePtr
    
    external fun clang_getCXTUResourceUsage(TU: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_disposeCXTUResourceUsage(usage: NativePtr): Unit
    
    external fun clang_getNullCursor(retValPlacement: NativePtr): NativePtr
    
    external fun clang_getTranslationUnitCursor(arg0: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_equalCursors(arg0: NativePtr, arg1: NativePtr): Int
    
    external fun clang_Cursor_isNull(cursor: NativePtr): Int
    
    external fun clang_hashCursor(arg0: NativePtr): Int
    
    external fun clang_getCursorKind(arg0: NativePtr): Int
    
    external fun clang_isDeclaration(arg0: Int): Int
    
    external fun clang_isReference(arg0: Int): Int
    
    external fun clang_isExpression(arg0: Int): Int
    
    external fun clang_isStatement(arg0: Int): Int
    
    external fun clang_isAttribute(arg0: Int): Int
    
    external fun clang_Cursor_hasAttrs(C: NativePtr): Int
    
    external fun clang_isInvalid(arg0: Int): Int
    
    external fun clang_isTranslationUnit(arg0: Int): Int
    
    external fun clang_isPreprocessing(arg0: Int): Int
    
    external fun clang_isUnexposed(arg0: Int): Int
    
    external fun clang_getCursorLinkage(cursor: NativePtr): Int
    
    external fun clang_getCursorVisibility(cursor: NativePtr): Int
    
    external fun clang_getCursorAvailability(cursor: NativePtr): Int
    
    external fun clang_getCursorPlatformAvailability(cursor: NativePtr, always_deprecated: NativePtr, deprecated_message: NativePtr, always_unavailable: NativePtr, unavailable_message: NativePtr, availability: NativePtr, availability_size: Int): Int
    
    external fun clang_disposeCXPlatformAvailability(availability: NativePtr): Unit
    
    external fun clang_getCursorLanguage(cursor: NativePtr): Int
    
    external fun clang_Cursor_getTranslationUnit(arg0: NativePtr): NativePtr
    
    external fun clang_createCXCursorSet(): NativePtr
    
    external fun clang_disposeCXCursorSet(cset: NativePtr): Unit
    
    external fun clang_CXCursorSet_contains(cset: NativePtr, cursor: NativePtr): Int
    
    external fun clang_CXCursorSet_insert(cset: NativePtr, cursor: NativePtr): Int
    
    external fun clang_getCursorSemanticParent(cursor: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getCursorLexicalParent(cursor: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getOverriddenCursors(cursor: NativePtr, overridden: NativePtr, num_overridden: NativePtr): Unit
    
    external fun clang_disposeOverriddenCursors(overridden: NativePtr): Unit
    
    external fun clang_getIncludedFile(cursor: NativePtr): NativePtr
    
    external fun clang_getCursor(arg0: NativePtr, arg1: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getCursorLocation(arg0: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getCursorExtent(arg0: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getCursorType(C: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getTypeSpelling(CT: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getTypedefDeclUnderlyingType(C: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getEnumDeclIntegerType(C: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getEnumConstantDeclValue(C: NativePtr): Long
    
    external fun clang_getEnumConstantDeclUnsignedValue(C: NativePtr): Long
    
    external fun clang_getFieldDeclBitWidth(C: NativePtr): Int
    
    external fun clang_Cursor_getNumArguments(C: NativePtr): Int
    
    external fun clang_Cursor_getArgument(C: NativePtr, i: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Cursor_getNumTemplateArguments(C: NativePtr): Int
    
    external fun clang_Cursor_getTemplateArgumentKind(C: NativePtr, I: Int): Int
    
    external fun clang_Cursor_getTemplateArgumentType(C: NativePtr, I: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Cursor_getTemplateArgumentValue(C: NativePtr, I: Int): Long
    
    external fun clang_Cursor_getTemplateArgumentUnsignedValue(C: NativePtr, I: Int): Long
    
    external fun clang_equalTypes(A: NativePtr, B: NativePtr): Int
    
    external fun clang_getCanonicalType(T: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_isConstQualifiedType(T: NativePtr): Int
    
    external fun clang_Cursor_isMacroFunctionLike(C: NativePtr): Int
    
    external fun clang_Cursor_isMacroBuiltin(C: NativePtr): Int
    
    external fun clang_Cursor_isFunctionInlined(C: NativePtr): Int
    
    external fun clang_isVolatileQualifiedType(T: NativePtr): Int
    
    external fun clang_isRestrictQualifiedType(T: NativePtr): Int
    
    external fun clang_getPointeeType(T: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getTypeDeclaration(T: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getDeclObjCTypeEncoding(C: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Type_getObjCEncoding(type: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getTypeKindSpelling(K: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getFunctionTypeCallingConv(T: NativePtr): Int
    
    external fun clang_getResultType(T: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getNumArgTypes(T: NativePtr): Int
    
    external fun clang_getArgType(T: NativePtr, i: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_isFunctionTypeVariadic(T: NativePtr): Int
    
    external fun clang_getCursorResultType(C: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_isPODType(T: NativePtr): Int
    
    external fun clang_getElementType(T: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getNumElements(T: NativePtr): Long
    
    external fun clang_getArrayElementType(T: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getArraySize(T: NativePtr): Long
    
    external fun clang_Type_getNamedType(T: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Type_getAlignOf(T: NativePtr): Long
    
    external fun clang_Type_getClassType(T: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Type_getSizeOf(T: NativePtr): Long
    
    external fun clang_Type_getOffsetOf(T: NativePtr, S: NativePtr): Long
    
    external fun clang_Cursor_getOffsetOfField(C: NativePtr): Long
    
    external fun clang_Cursor_isAnonymous(C: NativePtr): Int
    
    external fun clang_Type_getNumTemplateArguments(T: NativePtr): Int
    
    external fun clang_Type_getTemplateArgumentAsType(T: NativePtr, i: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Type_getCXXRefQualifier(T: NativePtr): Int
    
    external fun clang_Cursor_isBitField(C: NativePtr): Int
    
    external fun clang_isVirtualBase(arg0: NativePtr): Int
    
    external fun clang_getCXXAccessSpecifier(arg0: NativePtr): Int
    
    external fun clang_Cursor_getStorageClass(arg0: NativePtr): Int
    
    external fun clang_getNumOverloadedDecls(cursor: NativePtr): Int
    
    external fun clang_getOverloadedDecl(cursor: NativePtr, index: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getIBOutletCollectionType(arg0: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_visitChildren(parent: NativePtr, visitor: NativePtr, client_data: NativePtr): Int
    
    external fun clang_getCursorUSR(arg0: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_constructUSR_ObjCClass(class_name: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_constructUSR_ObjCCategory(class_name: NativePtr, category_name: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_constructUSR_ObjCProtocol(protocol_name: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_constructUSR_ObjCIvar(name: NativePtr, classUSR: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_constructUSR_ObjCMethod(name: NativePtr, isInstanceMethod: Int, classUSR: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_constructUSR_ObjCProperty(property: NativePtr, classUSR: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getCursorSpelling(arg0: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Cursor_getSpellingNameRange(arg0: NativePtr, pieceIndex: Int, options: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getCursorDisplayName(arg0: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getCursorReferenced(arg0: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getCursorDefinition(arg0: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_isCursorDefinition(arg0: NativePtr): Int
    
    external fun clang_getCanonicalCursor(arg0: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Cursor_getObjCSelectorIndex(arg0: NativePtr): Int
    
    external fun clang_Cursor_isDynamicCall(C: NativePtr): Int
    
    external fun clang_Cursor_getReceiverType(C: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Cursor_getObjCPropertyAttributes(C: NativePtr, reserved: Int): Int
    
    external fun clang_Cursor_getObjCDeclQualifiers(C: NativePtr): Int
    
    external fun clang_Cursor_isObjCOptional(C: NativePtr): Int
    
    external fun clang_Cursor_isVariadic(C: NativePtr): Int
    
    external fun clang_Cursor_getCommentRange(C: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Cursor_getRawCommentText(C: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Cursor_getBriefCommentText(C: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Cursor_getMangling(arg0: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Cursor_getCXXManglings(arg0: NativePtr): NativePtr
    
    external fun clang_Cursor_getModule(C: NativePtr): NativePtr
    
    external fun clang_getModuleForFile(arg0: NativePtr, arg1: NativePtr): NativePtr
    
    external fun clang_Module_getASTFile(Module: NativePtr): NativePtr
    
    external fun clang_Module_getParent(Module: NativePtr): NativePtr
    
    external fun clang_Module_getName(Module: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Module_getFullName(Module: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Module_isSystem(Module: NativePtr): Int
    
    external fun clang_Module_getNumTopLevelHeaders(arg0: NativePtr, Module: NativePtr): Int
    
    external fun clang_Module_getTopLevelHeader(arg0: NativePtr, Module: NativePtr, Index: Int): NativePtr
    
    external fun clang_CXXConstructor_isConvertingConstructor(C: NativePtr): Int
    
    external fun clang_CXXConstructor_isCopyConstructor(C: NativePtr): Int
    
    external fun clang_CXXConstructor_isDefaultConstructor(C: NativePtr): Int
    
    external fun clang_CXXConstructor_isMoveConstructor(C: NativePtr): Int
    
    external fun clang_CXXField_isMutable(C: NativePtr): Int
    
    external fun clang_CXXMethod_isDefaulted(C: NativePtr): Int
    
    external fun clang_CXXMethod_isPureVirtual(C: NativePtr): Int
    
    external fun clang_CXXMethod_isStatic(C: NativePtr): Int
    
    external fun clang_CXXMethod_isVirtual(C: NativePtr): Int
    
    external fun clang_CXXMethod_isConst(C: NativePtr): Int
    
    external fun clang_getTemplateCursorKind(C: NativePtr): Int
    
    external fun clang_getSpecializedCursorTemplate(C: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getCursorReferenceNameRange(C: NativePtr, NameFlags: Int, PieceIndex: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getTokenKind(arg0: NativePtr): Int
    
    external fun clang_getTokenSpelling(arg0: NativePtr, arg1: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getTokenLocation(arg0: NativePtr, arg1: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getTokenExtent(arg0: NativePtr, arg1: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_tokenize(TU: NativePtr, Range: NativePtr, Tokens: NativePtr, NumTokens: NativePtr): Unit
    
    external fun clang_annotateTokens(TU: NativePtr, Tokens: NativePtr, NumTokens: Int, Cursors: NativePtr): Unit
    
    external fun clang_disposeTokens(TU: NativePtr, Tokens: NativePtr, NumTokens: Int): Unit
    
    external fun clang_getCursorKindSpelling(Kind: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getDefinitionSpellingAndExtent(arg0: NativePtr, startBuf: NativePtr, endBuf: NativePtr, startLine: NativePtr, startColumn: NativePtr, endLine: NativePtr, endColumn: NativePtr): Unit
    
    external fun clang_enableStackTraces(): Unit
    
    external fun clang_executeOnThread(fn: NativePtr, user_data: NativePtr, stack_size: Int): Unit
    
    external fun clang_getCompletionChunkKind(completion_string: NativePtr, chunk_number: Int): Int
    
    external fun clang_getCompletionChunkText(completion_string: NativePtr, chunk_number: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getCompletionChunkCompletionString(completion_string: NativePtr, chunk_number: Int): NativePtr
    
    external fun clang_getNumCompletionChunks(completion_string: NativePtr): Int
    
    external fun clang_getCompletionPriority(completion_string: NativePtr): Int
    
    external fun clang_getCompletionAvailability(completion_string: NativePtr): Int
    
    external fun clang_getCompletionNumAnnotations(completion_string: NativePtr): Int
    
    external fun clang_getCompletionAnnotation(completion_string: NativePtr, annotation_number: Int, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getCompletionParent(completion_string: NativePtr, kind: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getCompletionBriefComment(completion_string: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getCursorCompletionString(cursor: NativePtr): NativePtr
    
    external fun clang_defaultCodeCompleteOptions(): Int
    
    external fun clang_codeCompleteAt(TU: NativePtr, complete_filename: NativePtr, complete_line: Int, complete_column: Int, unsaved_files: NativePtr, num_unsaved_files: Int, options: Int): NativePtr
    
    external fun clang_sortCodeCompletionResults(Results: NativePtr, NumResults: Int): Unit
    
    external fun clang_disposeCodeCompleteResults(Results: NativePtr): Unit
    
    external fun clang_codeCompleteGetNumDiagnostics(Results: NativePtr): Int
    
    external fun clang_codeCompleteGetDiagnostic(Results: NativePtr, Index: Int): NativePtr
    
    external fun clang_codeCompleteGetContexts(Results: NativePtr): Long
    
    external fun clang_codeCompleteGetContainerKind(Results: NativePtr, IsIncomplete: NativePtr): Int
    
    external fun clang_codeCompleteGetContainerUSR(Results: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_codeCompleteGetObjCSelector(Results: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_getClangVersion(retValPlacement: NativePtr): NativePtr
    
    external fun clang_toggleCrashRecovery(isEnabled: Int): Unit
    
    external fun clang_getInclusions(tu: NativePtr, visitor: NativePtr, client_data: NativePtr): Unit
    
    external fun clang_Cursor_Evaluate(C: NativePtr): NativePtr
    
    external fun clang_EvalResult_getKind(E: NativePtr): Int
    
    external fun clang_EvalResult_getAsInt(E: NativePtr): Int
    
    external fun clang_EvalResult_getAsDouble(E: NativePtr): Double
    
    external fun clang_EvalResult_getAsStr(E: NativePtr): NativePtr
    
    external fun clang_EvalResult_dispose(E: NativePtr): Unit
    
    external fun clang_getRemappings(path: NativePtr): NativePtr
    
    external fun clang_getRemappingsFromFileList(filePaths: NativePtr, numFiles: Int): NativePtr
    
    external fun clang_remap_getNumFiles(arg0: NativePtr): Int
    
    external fun clang_remap_getFilenames(arg0: NativePtr, index: Int, original: NativePtr, transformed: NativePtr): Unit
    
    external fun clang_remap_dispose(arg0: NativePtr): Unit
    
    external fun clang_findReferencesInFile(cursor: NativePtr, file: NativePtr, visitor: NativePtr): Int
    
    external fun clang_findIncludesInFile(TU: NativePtr, file: NativePtr, visitor: NativePtr): Int
    
    external fun clang_index_isEntityObjCContainerKind(arg0: Int): Int
    
    external fun clang_index_getObjCContainerDeclInfo(arg0: NativePtr): NativePtr
    
    external fun clang_index_getObjCInterfaceDeclInfo(arg0: NativePtr): NativePtr
    
    external fun clang_index_getObjCCategoryDeclInfo(arg0: NativePtr): NativePtr
    
    external fun clang_index_getObjCProtocolRefListInfo(arg0: NativePtr): NativePtr
    
    external fun clang_index_getObjCPropertyDeclInfo(arg0: NativePtr): NativePtr
    
    external fun clang_index_getIBOutletCollectionAttrInfo(arg0: NativePtr): NativePtr
    
    external fun clang_index_getCXXClassDeclInfo(arg0: NativePtr): NativePtr
    
    external fun clang_index_getClientContainer(arg0: NativePtr): NativePtr
    
    external fun clang_index_setClientContainer(arg0: NativePtr, arg1: NativePtr): Unit
    
    external fun clang_index_getClientEntity(arg0: NativePtr): NativePtr
    
    external fun clang_index_setClientEntity(arg0: NativePtr, arg1: NativePtr): Unit
    
    external fun clang_IndexAction_create(CIdx: NativePtr): NativePtr
    
    external fun clang_IndexAction_dispose(arg0: NativePtr): Unit
    
    external fun clang_indexSourceFile(arg0: NativePtr, client_data: NativePtr, index_callbacks: NativePtr, index_callbacks_size: Int, index_options: Int, source_filename: NativePtr, command_line_args: NativePtr, num_command_line_args: Int, unsaved_files: NativePtr, num_unsaved_files: Int, out_TU: NativePtr, TU_options: Int): Int
    
    external fun clang_indexSourceFileFullArgv(arg0: NativePtr, client_data: NativePtr, index_callbacks: NativePtr, index_callbacks_size: Int, index_options: Int, source_filename: NativePtr, command_line_args: NativePtr, num_command_line_args: Int, unsaved_files: NativePtr, num_unsaved_files: Int, out_TU: NativePtr, TU_options: Int): Int
    
    external fun clang_indexTranslationUnit(arg0: NativePtr, client_data: NativePtr, index_callbacks: NativePtr, index_callbacks_size: Int, index_options: Int, arg5: NativePtr): Int
    
    external fun clang_indexLoc_getFileLocation(loc: NativePtr, indexFile: NativePtr, file: NativePtr, line: NativePtr, column: NativePtr, offset: NativePtr): Unit
    
    external fun clang_indexLoc_getCXSourceLocation(loc: NativePtr, retValPlacement: NativePtr): NativePtr
    
    external fun clang_Type_visitFields(T: NativePtr, visitor: NativePtr, client_data: NativePtr): Int
    
}
