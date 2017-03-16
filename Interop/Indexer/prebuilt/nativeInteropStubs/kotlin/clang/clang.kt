package clang

import kotlinx.cinterop.*

fun asctime(arg0: CValuesRef<tm>?): CPointer<CInt8Var>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.asctime(_arg0)
        interpretCPointer<CInt8Var>(res)
    }
}

fun clock(): clock_t {
    val res = externals.clock()
    return res
}

fun ctime(arg0: CValuesRef<time_tVar>?): CPointer<CInt8Var>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.ctime(_arg0)
        interpretCPointer<CInt8Var>(res)
    }
}

fun difftime(arg0: time_t, arg1: time_t): Double {
    val _arg0 = arg0
    val _arg1 = arg1
    val res = externals.difftime(_arg0, _arg1)
    return res
}

fun getdate(arg0: String?): CPointer<tm>? {
    return memScoped {
        val _arg0 = arg0?.cstr?.getPointer(memScope).rawValue
        val res = externals.getdate(_arg0)
        interpretCPointer<tm>(res)
    }
}

fun gmtime(arg0: CValuesRef<time_tVar>?): CPointer<tm>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.gmtime(_arg0)
        interpretCPointer<tm>(res)
    }
}

fun localtime(arg0: CValuesRef<time_tVar>?): CPointer<tm>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.localtime(_arg0)
        interpretCPointer<tm>(res)
    }
}

fun mktime(arg0: CValuesRef<tm>?): time_t {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.mktime(_arg0)
        res
    }
}

fun strftime(arg0: CValuesRef<CInt8Var>?, arg1: size_t, arg2: String?, arg3: CValuesRef<tm>?): size_t {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val _arg1 = arg1
        val _arg2 = arg2?.cstr?.getPointer(memScope).rawValue
        val _arg3 = arg3?.getPointer(memScope).rawValue
        val res = externals.strftime(_arg0, _arg1, _arg2, _arg3)
        res
    }
}

fun strptime(arg0: String?, arg1: String?, arg2: CValuesRef<tm>?): CPointer<CInt8Var>? {
    return memScoped {
        val _arg0 = arg0?.cstr?.getPointer(memScope).rawValue
        val _arg1 = arg1?.cstr?.getPointer(memScope).rawValue
        val _arg2 = arg2?.getPointer(memScope).rawValue
        val res = externals.strptime(_arg0, _arg1, _arg2)
        interpretCPointer<CInt8Var>(res)
    }
}

fun time(arg0: CValuesRef<time_tVar>?): time_t {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.time(_arg0)
        res
    }
}

fun tzset(): Unit {
    val res = externals.tzset()
    return res
}

fun asctime_r(arg0: CValuesRef<tm>?, arg1: CValuesRef<CInt8Var>?): CPointer<CInt8Var>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val _arg1 = arg1?.getPointer(memScope).rawValue
        val res = externals.asctime_r(_arg0, _arg1)
        interpretCPointer<CInt8Var>(res)
    }
}

fun ctime_r(arg0: CValuesRef<time_tVar>?, arg1: CValuesRef<CInt8Var>?): CPointer<CInt8Var>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val _arg1 = arg1?.getPointer(memScope).rawValue
        val res = externals.ctime_r(_arg0, _arg1)
        interpretCPointer<CInt8Var>(res)
    }
}

fun gmtime_r(arg0: CValuesRef<time_tVar>?, arg1: CValuesRef<tm>?): CPointer<tm>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val _arg1 = arg1?.getPointer(memScope).rawValue
        val res = externals.gmtime_r(_arg0, _arg1)
        interpretCPointer<tm>(res)
    }
}

fun localtime_r(arg0: CValuesRef<time_tVar>?, arg1: CValuesRef<tm>?): CPointer<tm>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val _arg1 = arg1?.getPointer(memScope).rawValue
        val res = externals.localtime_r(_arg0, _arg1)
        interpretCPointer<tm>(res)
    }
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

fun timelocal(arg0: CValuesRef<tm>?): time_t {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.timelocal(_arg0)
        res
    }
}

fun timegm(arg0: CValuesRef<tm>?): time_t {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.timegm(_arg0)
        res
    }
}

fun nanosleep(__rqtp: CValuesRef<timespec>?, __rmtp: CValuesRef<timespec>?): Int {
    return memScoped {
        val ___rqtp = __rqtp?.getPointer(memScope).rawValue
        val ___rmtp = __rmtp?.getPointer(memScope).rawValue
        val res = externals.nanosleep(___rqtp, ___rmtp)
        res
    }
}

fun clock_getres(__clock_id: clockid_t, __res: CValuesRef<timespec>?): Int {
    return memScoped {
        val ___clock_id = __clock_id
        val ___res = __res?.getPointer(memScope).rawValue
        val res = externals.clock_getres(___clock_id, ___res)
        res
    }
}

fun clock_gettime(__clock_id: clockid_t, __tp: CValuesRef<timespec>?): Int {
    return memScoped {
        val ___clock_id = __clock_id
        val ___tp = __tp?.getPointer(memScope).rawValue
        val res = externals.clock_gettime(___clock_id, ___tp)
        res
    }
}

fun clock_gettime_nsec_np(__clock_id: clockid_t): __uint64_t {
    val ___clock_id = __clock_id
    val res = externals.clock_gettime_nsec_np(___clock_id)
    return res
}

fun clock_settime(__clock_id: clockid_t, __tp: CValuesRef<timespec>?): Int {
    return memScoped {
        val ___clock_id = __clock_id
        val ___tp = __tp?.getPointer(memScope).rawValue
        val res = externals.clock_settime(___clock_id, ___tp)
        res
    }
}

fun clang_getCString(string: CValue<CXString>): CPointer<CInt8Var>? {
    return memScoped {
        val _string = string.getPointer(memScope).rawValue
        val res = externals.clang_getCString(_string)
        interpretCPointer<CInt8Var>(res)
    }
}

fun clang_disposeString(string: CValue<CXString>): Unit {
    return memScoped {
        val _string = string.getPointer(memScope).rawValue
        val res = externals.clang_disposeString(_string)
        res
    }
}

fun clang_disposeStringSet(set: CValuesRef<CXStringSet>?): Unit {
    return memScoped {
        val _set = set?.getPointer(memScope).rawValue
        val res = externals.clang_disposeStringSet(_set)
        res
    }
}

fun clang_getBuildSessionTimestamp(): Long {
    val res = externals.clang_getBuildSessionTimestamp()
    return res
}

fun clang_VirtualFileOverlay_create(options: Int): CXVirtualFileOverlay? {
    val _options = options
    val res = externals.clang_VirtualFileOverlay_create(_options)
    return interpretCPointer<CXVirtualFileOverlayImpl>(res)
}

fun clang_VirtualFileOverlay_addFileMapping(arg0: CXVirtualFileOverlay?, virtualPath: String?, realPath: String?): CXErrorCode {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _virtualPath = virtualPath?.cstr?.getPointer(memScope).rawValue
        val _realPath = realPath?.cstr?.getPointer(memScope).rawValue
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

fun clang_VirtualFileOverlay_writeToBuffer(arg0: CXVirtualFileOverlay?, options: Int, out_buffer_ptr: CValuesRef<CPointerVar<CInt8Var>>?, out_buffer_size: CValuesRef<CInt32Var>?): CXErrorCode {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _options = options
        val _out_buffer_ptr = out_buffer_ptr?.getPointer(memScope).rawValue
        val _out_buffer_size = out_buffer_size?.getPointer(memScope).rawValue
        val res = externals.clang_VirtualFileOverlay_writeToBuffer(_arg0, _options, _out_buffer_ptr, _out_buffer_size)
        CXErrorCode.byValue(res)
    }
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
    return interpretCPointer<CXModuleMapDescriptorImpl>(res)
}

fun clang_ModuleMapDescriptor_setFrameworkModuleName(arg0: CXModuleMapDescriptor?, name: String?): CXErrorCode {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _name = name?.cstr?.getPointer(memScope).rawValue
        val res = externals.clang_ModuleMapDescriptor_setFrameworkModuleName(_arg0, _name)
        CXErrorCode.byValue(res)
    }
}

fun clang_ModuleMapDescriptor_setUmbrellaHeader(arg0: CXModuleMapDescriptor?, name: String?): CXErrorCode {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _name = name?.cstr?.getPointer(memScope).rawValue
        val res = externals.clang_ModuleMapDescriptor_setUmbrellaHeader(_arg0, _name)
        CXErrorCode.byValue(res)
    }
}

fun clang_ModuleMapDescriptor_writeToBuffer(arg0: CXModuleMapDescriptor?, options: Int, out_buffer_ptr: CValuesRef<CPointerVar<CInt8Var>>?, out_buffer_size: CValuesRef<CInt32Var>?): CXErrorCode {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _options = options
        val _out_buffer_ptr = out_buffer_ptr?.getPointer(memScope).rawValue
        val _out_buffer_size = out_buffer_size?.getPointer(memScope).rawValue
        val res = externals.clang_ModuleMapDescriptor_writeToBuffer(_arg0, _options, _out_buffer_ptr, _out_buffer_size)
        CXErrorCode.byValue(res)
    }
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
    return interpretCPointer<COpaque>(res)
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

fun clang_getFileName(SFile: CXFile?): CValue<CXString> {
    return memScoped {
        val _SFile = SFile.rawValue
        val res = externals.clang_getFileName(_SFile, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getFileTime(SFile: CXFile?): time_t {
    val _SFile = SFile.rawValue
    val res = externals.clang_getFileTime(_SFile)
    return res
}

fun clang_getFileUniqueID(file: CXFile?, outID: CValuesRef<CXFileUniqueID>?): Int {
    return memScoped {
        val _file = file.rawValue
        val _outID = outID?.getPointer(memScope).rawValue
        val res = externals.clang_getFileUniqueID(_file, _outID)
        res
    }
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
        val _file_name = file_name?.cstr?.getPointer(memScope).rawValue
        val res = externals.clang_getFile(_tu, _file_name)
        interpretCPointer<COpaque>(res)
    }
}

fun clang_File_isEqual(file1: CXFile?, file2: CXFile?): Int {
    val _file1 = file1.rawValue
    val _file2 = file2.rawValue
    val res = externals.clang_File_isEqual(_file1, _file2)
    return res
}

fun clang_getNullLocation(): CValue<CXSourceLocation> {
    return memScoped {
        val res = externals.clang_getNullLocation(alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

fun clang_equalLocations(loc1: CValue<CXSourceLocation>, loc2: CValue<CXSourceLocation>): Int {
    return memScoped {
        val _loc1 = loc1.getPointer(memScope).rawValue
        val _loc2 = loc2.getPointer(memScope).rawValue
        val res = externals.clang_equalLocations(_loc1, _loc2)
        res
    }
}

fun clang_getLocation(tu: CXTranslationUnit?, file: CXFile?, line: Int, column: Int): CValue<CXSourceLocation> {
    return memScoped {
        val _tu = tu.rawValue
        val _file = file.rawValue
        val _line = line
        val _column = column
        val res = externals.clang_getLocation(_tu, _file, _line, _column, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

fun clang_getLocationForOffset(tu: CXTranslationUnit?, file: CXFile?, offset: Int): CValue<CXSourceLocation> {
    return memScoped {
        val _tu = tu.rawValue
        val _file = file.rawValue
        val _offset = offset
        val res = externals.clang_getLocationForOffset(_tu, _file, _offset, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

fun clang_Location_isInSystemHeader(location: CValue<CXSourceLocation>): Int {
    return memScoped {
        val _location = location.getPointer(memScope).rawValue
        val res = externals.clang_Location_isInSystemHeader(_location)
        res
    }
}

fun clang_Location_isFromMainFile(location: CValue<CXSourceLocation>): Int {
    return memScoped {
        val _location = location.getPointer(memScope).rawValue
        val res = externals.clang_Location_isFromMainFile(_location)
        res
    }
}

fun clang_getNullRange(): CValue<CXSourceRange> {
    return memScoped {
        val res = externals.clang_getNullRange(alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

fun clang_getRange(begin: CValue<CXSourceLocation>, end: CValue<CXSourceLocation>): CValue<CXSourceRange> {
    return memScoped {
        val _begin = begin.getPointer(memScope).rawValue
        val _end = end.getPointer(memScope).rawValue
        val res = externals.clang_getRange(_begin, _end, alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

fun clang_equalRanges(range1: CValue<CXSourceRange>, range2: CValue<CXSourceRange>): Int {
    return memScoped {
        val _range1 = range1.getPointer(memScope).rawValue
        val _range2 = range2.getPointer(memScope).rawValue
        val res = externals.clang_equalRanges(_range1, _range2)
        res
    }
}

fun clang_Range_isNull(range: CValue<CXSourceRange>): Int {
    return memScoped {
        val _range = range.getPointer(memScope).rawValue
        val res = externals.clang_Range_isNull(_range)
        res
    }
}

fun clang_getExpansionLocation(location: CValue<CXSourceLocation>, file: CValuesRef<CXFileVar>?, line: CValuesRef<CInt32Var>?, column: CValuesRef<CInt32Var>?, offset: CValuesRef<CInt32Var>?): Unit {
    return memScoped {
        val _location = location.getPointer(memScope).rawValue
        val _file = file?.getPointer(memScope).rawValue
        val _line = line?.getPointer(memScope).rawValue
        val _column = column?.getPointer(memScope).rawValue
        val _offset = offset?.getPointer(memScope).rawValue
        val res = externals.clang_getExpansionLocation(_location, _file, _line, _column, _offset)
        res
    }
}

fun clang_getPresumedLocation(location: CValue<CXSourceLocation>, filename: CValuesRef<CXString>?, line: CValuesRef<CInt32Var>?, column: CValuesRef<CInt32Var>?): Unit {
    return memScoped {
        val _location = location.getPointer(memScope).rawValue
        val _filename = filename?.getPointer(memScope).rawValue
        val _line = line?.getPointer(memScope).rawValue
        val _column = column?.getPointer(memScope).rawValue
        val res = externals.clang_getPresumedLocation(_location, _filename, _line, _column)
        res
    }
}

fun clang_getInstantiationLocation(location: CValue<CXSourceLocation>, file: CValuesRef<CXFileVar>?, line: CValuesRef<CInt32Var>?, column: CValuesRef<CInt32Var>?, offset: CValuesRef<CInt32Var>?): Unit {
    return memScoped {
        val _location = location.getPointer(memScope).rawValue
        val _file = file?.getPointer(memScope).rawValue
        val _line = line?.getPointer(memScope).rawValue
        val _column = column?.getPointer(memScope).rawValue
        val _offset = offset?.getPointer(memScope).rawValue
        val res = externals.clang_getInstantiationLocation(_location, _file, _line, _column, _offset)
        res
    }
}

fun clang_getSpellingLocation(location: CValue<CXSourceLocation>, file: CValuesRef<CXFileVar>?, line: CValuesRef<CInt32Var>?, column: CValuesRef<CInt32Var>?, offset: CValuesRef<CInt32Var>?): Unit {
    return memScoped {
        val _location = location.getPointer(memScope).rawValue
        val _file = file?.getPointer(memScope).rawValue
        val _line = line?.getPointer(memScope).rawValue
        val _column = column?.getPointer(memScope).rawValue
        val _offset = offset?.getPointer(memScope).rawValue
        val res = externals.clang_getSpellingLocation(_location, _file, _line, _column, _offset)
        res
    }
}

fun clang_getFileLocation(location: CValue<CXSourceLocation>, file: CValuesRef<CXFileVar>?, line: CValuesRef<CInt32Var>?, column: CValuesRef<CInt32Var>?, offset: CValuesRef<CInt32Var>?): Unit {
    return memScoped {
        val _location = location.getPointer(memScope).rawValue
        val _file = file?.getPointer(memScope).rawValue
        val _line = line?.getPointer(memScope).rawValue
        val _column = column?.getPointer(memScope).rawValue
        val _offset = offset?.getPointer(memScope).rawValue
        val res = externals.clang_getFileLocation(_location, _file, _line, _column, _offset)
        res
    }
}

fun clang_getRangeStart(range: CValue<CXSourceRange>): CValue<CXSourceLocation> {
    return memScoped {
        val _range = range.getPointer(memScope).rawValue
        val res = externals.clang_getRangeStart(_range, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

fun clang_getRangeEnd(range: CValue<CXSourceRange>): CValue<CXSourceLocation> {
    return memScoped {
        val _range = range.getPointer(memScope).rawValue
        val res = externals.clang_getRangeEnd(_range, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

fun clang_getSkippedRanges(tu: CXTranslationUnit?, file: CXFile?): CPointer<CXSourceRangeList>? {
    val _tu = tu.rawValue
    val _file = file.rawValue
    val res = externals.clang_getSkippedRanges(_tu, _file)
    return interpretCPointer<CXSourceRangeList>(res)
}

fun clang_disposeSourceRangeList(ranges: CValuesRef<CXSourceRangeList>?): Unit {
    return memScoped {
        val _ranges = ranges?.getPointer(memScope).rawValue
        val res = externals.clang_disposeSourceRangeList(_ranges)
        res
    }
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
    return interpretCPointer<COpaque>(res)
}

fun clang_loadDiagnostics(file: String?, error: CValuesRef<CXLoadDiag_Error.Var>?, errorString: CValuesRef<CXString>?): CXDiagnosticSet? {
    return memScoped {
        val _file = file?.cstr?.getPointer(memScope).rawValue
        val _error = error?.getPointer(memScope).rawValue
        val _errorString = errorString?.getPointer(memScope).rawValue
        val res = externals.clang_loadDiagnostics(_file, _error, _errorString)
        interpretCPointer<COpaque>(res)
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
    return interpretCPointer<COpaque>(res)
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
    return interpretCPointer<COpaque>(res)
}

fun clang_getDiagnosticSetFromTU(Unit: CXTranslationUnit?): CXDiagnosticSet? {
    val _Unit = Unit.rawValue
    val res = externals.clang_getDiagnosticSetFromTU(_Unit)
    return interpretCPointer<COpaque>(res)
}

fun clang_disposeDiagnostic(Diagnostic: CXDiagnostic?): Unit {
    val _Diagnostic = Diagnostic.rawValue
    val res = externals.clang_disposeDiagnostic(_Diagnostic)
    return res
}

fun clang_formatDiagnostic(Diagnostic: CXDiagnostic?, Options: Int): CValue<CXString> {
    return memScoped {
        val _Diagnostic = Diagnostic.rawValue
        val _Options = Options
        val res = externals.clang_formatDiagnostic(_Diagnostic, _Options, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
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

fun clang_getDiagnosticLocation(arg0: CXDiagnostic?): CValue<CXSourceLocation> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val res = externals.clang_getDiagnosticLocation(_arg0, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

fun clang_getDiagnosticSpelling(arg0: CXDiagnostic?): CValue<CXString> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val res = externals.clang_getDiagnosticSpelling(_arg0, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getDiagnosticOption(Diag: CXDiagnostic?, Disable: CValuesRef<CXString>?): CValue<CXString> {
    return memScoped {
        val _Diag = Diag.rawValue
        val _Disable = Disable?.getPointer(memScope).rawValue
        val res = externals.clang_getDiagnosticOption(_Diag, _Disable, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getDiagnosticCategory(arg0: CXDiagnostic?): Int {
    val _arg0 = arg0.rawValue
    val res = externals.clang_getDiagnosticCategory(_arg0)
    return res
}

fun clang_getDiagnosticCategoryName(Category: Int): CValue<CXString> {
    return memScoped {
        val _Category = Category
        val res = externals.clang_getDiagnosticCategoryName(_Category, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getDiagnosticCategoryText(arg0: CXDiagnostic?): CValue<CXString> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val res = externals.clang_getDiagnosticCategoryText(_arg0, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getDiagnosticNumRanges(arg0: CXDiagnostic?): Int {
    val _arg0 = arg0.rawValue
    val res = externals.clang_getDiagnosticNumRanges(_arg0)
    return res
}

fun clang_getDiagnosticRange(Diagnostic: CXDiagnostic?, Range: Int): CValue<CXSourceRange> {
    return memScoped {
        val _Diagnostic = Diagnostic.rawValue
        val _Range = Range
        val res = externals.clang_getDiagnosticRange(_Diagnostic, _Range, alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

fun clang_getDiagnosticNumFixIts(Diagnostic: CXDiagnostic?): Int {
    val _Diagnostic = Diagnostic.rawValue
    val res = externals.clang_getDiagnosticNumFixIts(_Diagnostic)
    return res
}

fun clang_getDiagnosticFixIt(Diagnostic: CXDiagnostic?, FixIt: Int, ReplacementRange: CValuesRef<CXSourceRange>?): CValue<CXString> {
    return memScoped {
        val _Diagnostic = Diagnostic.rawValue
        val _FixIt = FixIt
        val _ReplacementRange = ReplacementRange?.getPointer(memScope).rawValue
        val res = externals.clang_getDiagnosticFixIt(_Diagnostic, _FixIt, _ReplacementRange, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getTranslationUnitSpelling(CTUnit: CXTranslationUnit?): CValue<CXString> {
    return memScoped {
        val _CTUnit = CTUnit.rawValue
        val res = externals.clang_getTranslationUnitSpelling(_CTUnit, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_createTranslationUnitFromSourceFile(CIdx: CXIndex?, source_filename: String?, num_clang_command_line_args: Int, clang_command_line_args: CValuesRef<CPointerVar<CInt8Var>>?, num_unsaved_files: Int, unsaved_files: CValuesRef<CXUnsavedFile>?): CXTranslationUnit? {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _source_filename = source_filename?.cstr?.getPointer(memScope).rawValue
        val _num_clang_command_line_args = num_clang_command_line_args
        val _clang_command_line_args = clang_command_line_args?.getPointer(memScope).rawValue
        val _num_unsaved_files = num_unsaved_files
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val res = externals.clang_createTranslationUnitFromSourceFile(_CIdx, _source_filename, _num_clang_command_line_args, _clang_command_line_args, _num_unsaved_files, _unsaved_files)
        interpretCPointer<CXTranslationUnitImpl>(res)
    }
}

fun clang_createTranslationUnit(CIdx: CXIndex?, ast_filename: String?): CXTranslationUnit? {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _ast_filename = ast_filename?.cstr?.getPointer(memScope).rawValue
        val res = externals.clang_createTranslationUnit(_CIdx, _ast_filename)
        interpretCPointer<CXTranslationUnitImpl>(res)
    }
}

fun clang_createTranslationUnit2(CIdx: CXIndex?, ast_filename: String?, out_TU: CValuesRef<CXTranslationUnitVar>?): CXErrorCode {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _ast_filename = ast_filename?.cstr?.getPointer(memScope).rawValue
        val _out_TU = out_TU?.getPointer(memScope).rawValue
        val res = externals.clang_createTranslationUnit2(_CIdx, _ast_filename, _out_TU)
        CXErrorCode.byValue(res)
    }
}

fun clang_defaultEditingTranslationUnitOptions(): Int {
    val res = externals.clang_defaultEditingTranslationUnitOptions()
    return res
}

fun clang_parseTranslationUnit(CIdx: CXIndex?, source_filename: String?, command_line_args: CValuesRef<CPointerVar<CInt8Var>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, options: Int): CXTranslationUnit? {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _source_filename = source_filename?.cstr?.getPointer(memScope).rawValue
        val _command_line_args = command_line_args?.getPointer(memScope).rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val _num_unsaved_files = num_unsaved_files
        val _options = options
        val res = externals.clang_parseTranslationUnit(_CIdx, _source_filename, _command_line_args, _num_command_line_args, _unsaved_files, _num_unsaved_files, _options)
        interpretCPointer<CXTranslationUnitImpl>(res)
    }
}

fun clang_parseTranslationUnit2(CIdx: CXIndex?, source_filename: String?, command_line_args: CValuesRef<CPointerVar<CInt8Var>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, options: Int, out_TU: CValuesRef<CXTranslationUnitVar>?): CXErrorCode {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _source_filename = source_filename?.cstr?.getPointer(memScope).rawValue
        val _command_line_args = command_line_args?.getPointer(memScope).rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val _num_unsaved_files = num_unsaved_files
        val _options = options
        val _out_TU = out_TU?.getPointer(memScope).rawValue
        val res = externals.clang_parseTranslationUnit2(_CIdx, _source_filename, _command_line_args, _num_command_line_args, _unsaved_files, _num_unsaved_files, _options, _out_TU)
        CXErrorCode.byValue(res)
    }
}

fun clang_parseTranslationUnit2FullArgv(CIdx: CXIndex?, source_filename: String?, command_line_args: CValuesRef<CPointerVar<CInt8Var>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, options: Int, out_TU: CValuesRef<CXTranslationUnitVar>?): CXErrorCode {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _source_filename = source_filename?.cstr?.getPointer(memScope).rawValue
        val _command_line_args = command_line_args?.getPointer(memScope).rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val _num_unsaved_files = num_unsaved_files
        val _options = options
        val _out_TU = out_TU?.getPointer(memScope).rawValue
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
        val _FileName = FileName?.cstr?.getPointer(memScope).rawValue
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

fun clang_reparseTranslationUnit(TU: CXTranslationUnit?, num_unsaved_files: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, options: Int): Int {
    return memScoped {
        val _TU = TU.rawValue
        val _num_unsaved_files = num_unsaved_files
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val _options = options
        val res = externals.clang_reparseTranslationUnit(_TU, _num_unsaved_files, _unsaved_files, _options)
        res
    }
}

fun clang_getTUResourceUsageName(kind: CXTUResourceUsageKind): CPointer<CInt8Var>? {
    val _kind = kind.value
    val res = externals.clang_getTUResourceUsageName(_kind)
    return interpretCPointer<CInt8Var>(res)
}

fun clang_getCXTUResourceUsage(TU: CXTranslationUnit?): CValue<CXTUResourceUsage> {
    return memScoped {
        val _TU = TU.rawValue
        val res = externals.clang_getCXTUResourceUsage(_TU, alloc<CXTUResourceUsage>().rawPtr)
        interpretPointed<CXTUResourceUsage>(res).readValue()
    }
}

fun clang_disposeCXTUResourceUsage(usage: CValue<CXTUResourceUsage>): Unit {
    return memScoped {
        val _usage = usage.getPointer(memScope).rawValue
        val res = externals.clang_disposeCXTUResourceUsage(_usage)
        res
    }
}

fun clang_getNullCursor(): CValue<CXCursor> {
    return memScoped {
        val res = externals.clang_getNullCursor(alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

fun clang_getTranslationUnitCursor(arg0: CXTranslationUnit?): CValue<CXCursor> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val res = externals.clang_getTranslationUnitCursor(_arg0, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

fun clang_equalCursors(arg0: CValue<CXCursor>, arg1: CValue<CXCursor>): Int {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val _arg1 = arg1.getPointer(memScope).rawValue
        val res = externals.clang_equalCursors(_arg0, _arg1)
        res
    }
}

fun clang_Cursor_isNull(cursor: CValue<CXCursor>): Int {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_isNull(_cursor)
        res
    }
}

fun clang_hashCursor(arg0: CValue<CXCursor>): Int {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_hashCursor(_arg0)
        res
    }
}

fun clang_getCursorKind(arg0: CValue<CXCursor>): CXCursorKind {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_getCursorKind(_arg0)
        CXCursorKind.byValue(res)
    }
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

fun clang_Cursor_hasAttrs(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_hasAttrs(_C)
        res
    }
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

fun clang_getCursorLinkage(cursor: CValue<CXCursor>): CXLinkageKind {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = externals.clang_getCursorLinkage(_cursor)
        CXLinkageKind.byValue(res)
    }
}

fun clang_getCursorVisibility(cursor: CValue<CXCursor>): CXVisibilityKind {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = externals.clang_getCursorVisibility(_cursor)
        CXVisibilityKind.byValue(res)
    }
}

fun clang_getCursorAvailability(cursor: CValue<CXCursor>): CXAvailabilityKind {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = externals.clang_getCursorAvailability(_cursor)
        CXAvailabilityKind.byValue(res)
    }
}

fun clang_getCursorPlatformAvailability(cursor: CValue<CXCursor>, always_deprecated: CValuesRef<CInt32Var>?, deprecated_message: CValuesRef<CXString>?, always_unavailable: CValuesRef<CInt32Var>?, unavailable_message: CValuesRef<CXString>?, availability: CValuesRef<CXPlatformAvailability>?, availability_size: Int): Int {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val _always_deprecated = always_deprecated?.getPointer(memScope).rawValue
        val _deprecated_message = deprecated_message?.getPointer(memScope).rawValue
        val _always_unavailable = always_unavailable?.getPointer(memScope).rawValue
        val _unavailable_message = unavailable_message?.getPointer(memScope).rawValue
        val _availability = availability?.getPointer(memScope).rawValue
        val _availability_size = availability_size
        val res = externals.clang_getCursorPlatformAvailability(_cursor, _always_deprecated, _deprecated_message, _always_unavailable, _unavailable_message, _availability, _availability_size)
        res
    }
}

fun clang_disposeCXPlatformAvailability(availability: CValuesRef<CXPlatformAvailability>?): Unit {
    return memScoped {
        val _availability = availability?.getPointer(memScope).rawValue
        val res = externals.clang_disposeCXPlatformAvailability(_availability)
        res
    }
}

fun clang_getCursorLanguage(cursor: CValue<CXCursor>): CXLanguageKind {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = externals.clang_getCursorLanguage(_cursor)
        CXLanguageKind.byValue(res)
    }
}

fun clang_Cursor_getTranslationUnit(arg0: CValue<CXCursor>): CXTranslationUnit? {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_getTranslationUnit(_arg0)
        interpretCPointer<CXTranslationUnitImpl>(res)
    }
}

fun clang_createCXCursorSet(): CXCursorSet? {
    val res = externals.clang_createCXCursorSet()
    return interpretCPointer<CXCursorSetImpl>(res)
}

fun clang_disposeCXCursorSet(cset: CXCursorSet?): Unit {
    val _cset = cset.rawValue
    val res = externals.clang_disposeCXCursorSet(_cset)
    return res
}

fun clang_CXCursorSet_contains(cset: CXCursorSet?, cursor: CValue<CXCursor>): Int {
    return memScoped {
        val _cset = cset.rawValue
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = externals.clang_CXCursorSet_contains(_cset, _cursor)
        res
    }
}

fun clang_CXCursorSet_insert(cset: CXCursorSet?, cursor: CValue<CXCursor>): Int {
    return memScoped {
        val _cset = cset.rawValue
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = externals.clang_CXCursorSet_insert(_cset, _cursor)
        res
    }
}

fun clang_getCursorSemanticParent(cursor: CValue<CXCursor>): CValue<CXCursor> {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = externals.clang_getCursorSemanticParent(_cursor, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

fun clang_getCursorLexicalParent(cursor: CValue<CXCursor>): CValue<CXCursor> {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = externals.clang_getCursorLexicalParent(_cursor, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

fun clang_getOverriddenCursors(cursor: CValue<CXCursor>, overridden: CValuesRef<CPointerVar<CXCursor>>?, num_overridden: CValuesRef<CInt32Var>?): Unit {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val _overridden = overridden?.getPointer(memScope).rawValue
        val _num_overridden = num_overridden?.getPointer(memScope).rawValue
        val res = externals.clang_getOverriddenCursors(_cursor, _overridden, _num_overridden)
        res
    }
}

fun clang_disposeOverriddenCursors(overridden: CValuesRef<CXCursor>?): Unit {
    return memScoped {
        val _overridden = overridden?.getPointer(memScope).rawValue
        val res = externals.clang_disposeOverriddenCursors(_overridden)
        res
    }
}

fun clang_getIncludedFile(cursor: CValue<CXCursor>): CXFile? {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = externals.clang_getIncludedFile(_cursor)
        interpretCPointer<COpaque>(res)
    }
}

fun clang_getCursor(arg0: CXTranslationUnit?, arg1: CValue<CXSourceLocation>): CValue<CXCursor> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _arg1 = arg1.getPointer(memScope).rawValue
        val res = externals.clang_getCursor(_arg0, _arg1, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

fun clang_getCursorLocation(arg0: CValue<CXCursor>): CValue<CXSourceLocation> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_getCursorLocation(_arg0, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

fun clang_getCursorExtent(arg0: CValue<CXCursor>): CValue<CXSourceRange> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_getCursorExtent(_arg0, alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

fun clang_getCursorType(C: CValue<CXCursor>): CValue<CXType> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_getCursorType(_C, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_getTypeSpelling(CT: CValue<CXType>): CValue<CXString> {
    return memScoped {
        val _CT = CT.getPointer(memScope).rawValue
        val res = externals.clang_getTypeSpelling(_CT, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getTypedefDeclUnderlyingType(C: CValue<CXCursor>): CValue<CXType> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_getTypedefDeclUnderlyingType(_C, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_getEnumDeclIntegerType(C: CValue<CXCursor>): CValue<CXType> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_getEnumDeclIntegerType(_C, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_getEnumConstantDeclValue(C: CValue<CXCursor>): Long {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_getEnumConstantDeclValue(_C)
        res
    }
}

fun clang_getEnumConstantDeclUnsignedValue(C: CValue<CXCursor>): Long {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_getEnumConstantDeclUnsignedValue(_C)
        res
    }
}

fun clang_getFieldDeclBitWidth(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_getFieldDeclBitWidth(_C)
        res
    }
}

fun clang_Cursor_getNumArguments(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_getNumArguments(_C)
        res
    }
}

fun clang_Cursor_getArgument(C: CValue<CXCursor>, i: Int): CValue<CXCursor> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val _i = i
        val res = externals.clang_Cursor_getArgument(_C, _i, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

fun clang_Cursor_getNumTemplateArguments(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_getNumTemplateArguments(_C)
        res
    }
}

fun clang_Cursor_getTemplateArgumentKind(C: CValue<CXCursor>, I: Int): CXTemplateArgumentKind {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val _I = I
        val res = externals.clang_Cursor_getTemplateArgumentKind(_C, _I)
        CXTemplateArgumentKind.byValue(res)
    }
}

fun clang_Cursor_getTemplateArgumentType(C: CValue<CXCursor>, I: Int): CValue<CXType> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val _I = I
        val res = externals.clang_Cursor_getTemplateArgumentType(_C, _I, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_Cursor_getTemplateArgumentValue(C: CValue<CXCursor>, I: Int): Long {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val _I = I
        val res = externals.clang_Cursor_getTemplateArgumentValue(_C, _I)
        res
    }
}

fun clang_Cursor_getTemplateArgumentUnsignedValue(C: CValue<CXCursor>, I: Int): Long {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val _I = I
        val res = externals.clang_Cursor_getTemplateArgumentUnsignedValue(_C, _I)
        res
    }
}

fun clang_equalTypes(A: CValue<CXType>, B: CValue<CXType>): Int {
    return memScoped {
        val _A = A.getPointer(memScope).rawValue
        val _B = B.getPointer(memScope).rawValue
        val res = externals.clang_equalTypes(_A, _B)
        res
    }
}

fun clang_getCanonicalType(T: CValue<CXType>): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_getCanonicalType(_T, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_isConstQualifiedType(T: CValue<CXType>): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_isConstQualifiedType(_T)
        res
    }
}

fun clang_Cursor_isMacroFunctionLike(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_isMacroFunctionLike(_C)
        res
    }
}

fun clang_Cursor_isMacroBuiltin(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_isMacroBuiltin(_C)
        res
    }
}

fun clang_Cursor_isFunctionInlined(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_isFunctionInlined(_C)
        res
    }
}

fun clang_isVolatileQualifiedType(T: CValue<CXType>): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_isVolatileQualifiedType(_T)
        res
    }
}

fun clang_isRestrictQualifiedType(T: CValue<CXType>): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_isRestrictQualifiedType(_T)
        res
    }
}

fun clang_getPointeeType(T: CValue<CXType>): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_getPointeeType(_T, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_getTypeDeclaration(T: CValue<CXType>): CValue<CXCursor> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_getTypeDeclaration(_T, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

fun clang_getDeclObjCTypeEncoding(C: CValue<CXCursor>): CValue<CXString> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_getDeclObjCTypeEncoding(_C, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_Type_getObjCEncoding(type: CValue<CXType>): CValue<CXString> {
    return memScoped {
        val _type = type.getPointer(memScope).rawValue
        val res = externals.clang_Type_getObjCEncoding(_type, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getTypeKindSpelling(K: CXTypeKind): CValue<CXString> {
    return memScoped {
        val _K = K.value
        val res = externals.clang_getTypeKindSpelling(_K, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getFunctionTypeCallingConv(T: CValue<CXType>): CXCallingConv {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_getFunctionTypeCallingConv(_T)
        CXCallingConv.byValue(res)
    }
}

fun clang_getResultType(T: CValue<CXType>): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_getResultType(_T, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_getNumArgTypes(T: CValue<CXType>): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_getNumArgTypes(_T)
        res
    }
}

fun clang_getArgType(T: CValue<CXType>, i: Int): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val _i = i
        val res = externals.clang_getArgType(_T, _i, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_isFunctionTypeVariadic(T: CValue<CXType>): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_isFunctionTypeVariadic(_T)
        res
    }
}

fun clang_getCursorResultType(C: CValue<CXCursor>): CValue<CXType> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_getCursorResultType(_C, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_isPODType(T: CValue<CXType>): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_isPODType(_T)
        res
    }
}

fun clang_getElementType(T: CValue<CXType>): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_getElementType(_T, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_getNumElements(T: CValue<CXType>): Long {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_getNumElements(_T)
        res
    }
}

fun clang_getArrayElementType(T: CValue<CXType>): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_getArrayElementType(_T, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_getArraySize(T: CValue<CXType>): Long {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_getArraySize(_T)
        res
    }
}

fun clang_Type_getNamedType(T: CValue<CXType>): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_Type_getNamedType(_T, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_Type_getAlignOf(T: CValue<CXType>): Long {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_Type_getAlignOf(_T)
        res
    }
}

fun clang_Type_getClassType(T: CValue<CXType>): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_Type_getClassType(_T, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_Type_getSizeOf(T: CValue<CXType>): Long {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_Type_getSizeOf(_T)
        res
    }
}

fun clang_Type_getOffsetOf(T: CValue<CXType>, S: String?): Long {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val _S = S?.cstr?.getPointer(memScope).rawValue
        val res = externals.clang_Type_getOffsetOf(_T, _S)
        res
    }
}

fun clang_Cursor_getOffsetOfField(C: CValue<CXCursor>): Long {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_getOffsetOfField(_C)
        res
    }
}

fun clang_Cursor_isAnonymous(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_isAnonymous(_C)
        res
    }
}

fun clang_Type_getNumTemplateArguments(T: CValue<CXType>): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_Type_getNumTemplateArguments(_T)
        res
    }
}

fun clang_Type_getTemplateArgumentAsType(T: CValue<CXType>, i: Int): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val _i = i
        val res = externals.clang_Type_getTemplateArgumentAsType(_T, _i, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_Type_getCXXRefQualifier(T: CValue<CXType>): CXRefQualifierKind {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = externals.clang_Type_getCXXRefQualifier(_T)
        res
    }
}

fun clang_Cursor_isBitField(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_isBitField(_C)
        res
    }
}

fun clang_isVirtualBase(arg0: CValue<CXCursor>): Int {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_isVirtualBase(_arg0)
        res
    }
}

fun clang_getCXXAccessSpecifier(arg0: CValue<CXCursor>): CX_CXXAccessSpecifier {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_getCXXAccessSpecifier(_arg0)
        CX_CXXAccessSpecifier.byValue(res)
    }
}

fun clang_Cursor_getStorageClass(arg0: CValue<CXCursor>): CX_StorageClass {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_getStorageClass(_arg0)
        CX_StorageClass.byValue(res)
    }
}

fun clang_getNumOverloadedDecls(cursor: CValue<CXCursor>): Int {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = externals.clang_getNumOverloadedDecls(_cursor)
        res
    }
}

fun clang_getOverloadedDecl(cursor: CValue<CXCursor>, index: Int): CValue<CXCursor> {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val _index = index
        val res = externals.clang_getOverloadedDecl(_cursor, _index, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

fun clang_getIBOutletCollectionType(arg0: CValue<CXCursor>): CValue<CXType> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_getIBOutletCollectionType(_arg0, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_visitChildren(parent: CValue<CXCursor>, visitor: CXCursorVisitor?, client_data: CXClientData?): Int {
    return memScoped {
        val _parent = parent.getPointer(memScope).rawValue
        val _visitor = visitor.rawValue
        val _client_data = client_data.rawValue
        val res = externals.clang_visitChildren(_parent, _visitor, _client_data)
        res
    }
}

fun clang_getCursorUSR(arg0: CValue<CXCursor>): CValue<CXString> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_getCursorUSR(_arg0, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_constructUSR_ObjCClass(class_name: String?): CValue<CXString> {
    return memScoped {
        val _class_name = class_name?.cstr?.getPointer(memScope).rawValue
        val res = externals.clang_constructUSR_ObjCClass(_class_name, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_constructUSR_ObjCCategory(class_name: String?, category_name: String?): CValue<CXString> {
    return memScoped {
        val _class_name = class_name?.cstr?.getPointer(memScope).rawValue
        val _category_name = category_name?.cstr?.getPointer(memScope).rawValue
        val res = externals.clang_constructUSR_ObjCCategory(_class_name, _category_name, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_constructUSR_ObjCProtocol(protocol_name: String?): CValue<CXString> {
    return memScoped {
        val _protocol_name = protocol_name?.cstr?.getPointer(memScope).rawValue
        val res = externals.clang_constructUSR_ObjCProtocol(_protocol_name, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_constructUSR_ObjCIvar(name: String?, classUSR: CValue<CXString>): CValue<CXString> {
    return memScoped {
        val _name = name?.cstr?.getPointer(memScope).rawValue
        val _classUSR = classUSR.getPointer(memScope).rawValue
        val res = externals.clang_constructUSR_ObjCIvar(_name, _classUSR, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_constructUSR_ObjCMethod(name: String?, isInstanceMethod: Int, classUSR: CValue<CXString>): CValue<CXString> {
    return memScoped {
        val _name = name?.cstr?.getPointer(memScope).rawValue
        val _isInstanceMethod = isInstanceMethod
        val _classUSR = classUSR.getPointer(memScope).rawValue
        val res = externals.clang_constructUSR_ObjCMethod(_name, _isInstanceMethod, _classUSR, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_constructUSR_ObjCProperty(property: String?, classUSR: CValue<CXString>): CValue<CXString> {
    return memScoped {
        val _property = property?.cstr?.getPointer(memScope).rawValue
        val _classUSR = classUSR.getPointer(memScope).rawValue
        val res = externals.clang_constructUSR_ObjCProperty(_property, _classUSR, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getCursorSpelling(arg0: CValue<CXCursor>): CValue<CXString> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_getCursorSpelling(_arg0, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_Cursor_getSpellingNameRange(arg0: CValue<CXCursor>, pieceIndex: Int, options: Int): CValue<CXSourceRange> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val _pieceIndex = pieceIndex
        val _options = options
        val res = externals.clang_Cursor_getSpellingNameRange(_arg0, _pieceIndex, _options, alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

fun clang_getCursorDisplayName(arg0: CValue<CXCursor>): CValue<CXString> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_getCursorDisplayName(_arg0, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getCursorReferenced(arg0: CValue<CXCursor>): CValue<CXCursor> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_getCursorReferenced(_arg0, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

fun clang_getCursorDefinition(arg0: CValue<CXCursor>): CValue<CXCursor> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_getCursorDefinition(_arg0, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

fun clang_isCursorDefinition(arg0: CValue<CXCursor>): Int {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_isCursorDefinition(_arg0)
        res
    }
}

fun clang_getCanonicalCursor(arg0: CValue<CXCursor>): CValue<CXCursor> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_getCanonicalCursor(_arg0, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

fun clang_Cursor_getObjCSelectorIndex(arg0: CValue<CXCursor>): Int {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_getObjCSelectorIndex(_arg0)
        res
    }
}

fun clang_Cursor_isDynamicCall(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_isDynamicCall(_C)
        res
    }
}

fun clang_Cursor_getReceiverType(C: CValue<CXCursor>): CValue<CXType> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_getReceiverType(_C, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

fun clang_Cursor_getObjCPropertyAttributes(C: CValue<CXCursor>, reserved: Int): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val _reserved = reserved
        val res = externals.clang_Cursor_getObjCPropertyAttributes(_C, _reserved)
        res
    }
}

fun clang_Cursor_getObjCDeclQualifiers(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_getObjCDeclQualifiers(_C)
        res
    }
}

fun clang_Cursor_isObjCOptional(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_isObjCOptional(_C)
        res
    }
}

fun clang_Cursor_isVariadic(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_isVariadic(_C)
        res
    }
}

fun clang_Cursor_getCommentRange(C: CValue<CXCursor>): CValue<CXSourceRange> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_getCommentRange(_C, alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

fun clang_Cursor_getRawCommentText(C: CValue<CXCursor>): CValue<CXString> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_getRawCommentText(_C, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_Cursor_getBriefCommentText(C: CValue<CXCursor>): CValue<CXString> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_getBriefCommentText(_C, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_Cursor_getMangling(arg0: CValue<CXCursor>): CValue<CXString> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_getMangling(_arg0, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_Cursor_getCXXManglings(arg0: CValue<CXCursor>): CPointer<CXStringSet>? {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_getCXXManglings(_arg0)
        interpretCPointer<CXStringSet>(res)
    }
}

fun clang_Cursor_getModule(C: CValue<CXCursor>): CXModule? {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_getModule(_C)
        interpretCPointer<COpaque>(res)
    }
}

fun clang_getModuleForFile(arg0: CXTranslationUnit?, arg1: CXFile?): CXModule? {
    val _arg0 = arg0.rawValue
    val _arg1 = arg1.rawValue
    val res = externals.clang_getModuleForFile(_arg0, _arg1)
    return interpretCPointer<COpaque>(res)
}

fun clang_Module_getASTFile(Module: CXModule?): CXFile? {
    val _Module = Module.rawValue
    val res = externals.clang_Module_getASTFile(_Module)
    return interpretCPointer<COpaque>(res)
}

fun clang_Module_getParent(Module: CXModule?): CXModule? {
    val _Module = Module.rawValue
    val res = externals.clang_Module_getParent(_Module)
    return interpretCPointer<COpaque>(res)
}

fun clang_Module_getName(Module: CXModule?): CValue<CXString> {
    return memScoped {
        val _Module = Module.rawValue
        val res = externals.clang_Module_getName(_Module, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_Module_getFullName(Module: CXModule?): CValue<CXString> {
    return memScoped {
        val _Module = Module.rawValue
        val res = externals.clang_Module_getFullName(_Module, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
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
    return interpretCPointer<COpaque>(res)
}

fun clang_CXXConstructor_isConvertingConstructor(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_CXXConstructor_isConvertingConstructor(_C)
        res
    }
}

fun clang_CXXConstructor_isCopyConstructor(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_CXXConstructor_isCopyConstructor(_C)
        res
    }
}

fun clang_CXXConstructor_isDefaultConstructor(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_CXXConstructor_isDefaultConstructor(_C)
        res
    }
}

fun clang_CXXConstructor_isMoveConstructor(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_CXXConstructor_isMoveConstructor(_C)
        res
    }
}

fun clang_CXXField_isMutable(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_CXXField_isMutable(_C)
        res
    }
}

fun clang_CXXMethod_isDefaulted(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_CXXMethod_isDefaulted(_C)
        res
    }
}

fun clang_CXXMethod_isPureVirtual(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_CXXMethod_isPureVirtual(_C)
        res
    }
}

fun clang_CXXMethod_isStatic(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_CXXMethod_isStatic(_C)
        res
    }
}

fun clang_CXXMethod_isVirtual(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_CXXMethod_isVirtual(_C)
        res
    }
}

fun clang_CXXMethod_isConst(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_CXXMethod_isConst(_C)
        res
    }
}

fun clang_getTemplateCursorKind(C: CValue<CXCursor>): CXCursorKind {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_getTemplateCursorKind(_C)
        CXCursorKind.byValue(res)
    }
}

fun clang_getSpecializedCursorTemplate(C: CValue<CXCursor>): CValue<CXCursor> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_getSpecializedCursorTemplate(_C, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

fun clang_getCursorReferenceNameRange(C: CValue<CXCursor>, NameFlags: Int, PieceIndex: Int): CValue<CXSourceRange> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val _NameFlags = NameFlags
        val _PieceIndex = PieceIndex
        val res = externals.clang_getCursorReferenceNameRange(_C, _NameFlags, _PieceIndex, alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

fun clang_getTokenKind(arg0: CValue<CXToken>): CXTokenKind {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = externals.clang_getTokenKind(_arg0)
        CXTokenKind.byValue(res)
    }
}

fun clang_getTokenSpelling(arg0: CXTranslationUnit?, arg1: CValue<CXToken>): CValue<CXString> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _arg1 = arg1.getPointer(memScope).rawValue
        val res = externals.clang_getTokenSpelling(_arg0, _arg1, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getTokenLocation(arg0: CXTranslationUnit?, arg1: CValue<CXToken>): CValue<CXSourceLocation> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _arg1 = arg1.getPointer(memScope).rawValue
        val res = externals.clang_getTokenLocation(_arg0, _arg1, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

fun clang_getTokenExtent(arg0: CXTranslationUnit?, arg1: CValue<CXToken>): CValue<CXSourceRange> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _arg1 = arg1.getPointer(memScope).rawValue
        val res = externals.clang_getTokenExtent(_arg0, _arg1, alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

fun clang_tokenize(TU: CXTranslationUnit?, Range: CValue<CXSourceRange>, Tokens: CValuesRef<CPointerVar<CXToken>>?, NumTokens: CValuesRef<CInt32Var>?): Unit {
    return memScoped {
        val _TU = TU.rawValue
        val _Range = Range.getPointer(memScope).rawValue
        val _Tokens = Tokens?.getPointer(memScope).rawValue
        val _NumTokens = NumTokens?.getPointer(memScope).rawValue
        val res = externals.clang_tokenize(_TU, _Range, _Tokens, _NumTokens)
        res
    }
}

fun clang_annotateTokens(TU: CXTranslationUnit?, Tokens: CValuesRef<CXToken>?, NumTokens: Int, Cursors: CValuesRef<CXCursor>?): Unit {
    return memScoped {
        val _TU = TU.rawValue
        val _Tokens = Tokens?.getPointer(memScope).rawValue
        val _NumTokens = NumTokens
        val _Cursors = Cursors?.getPointer(memScope).rawValue
        val res = externals.clang_annotateTokens(_TU, _Tokens, _NumTokens, _Cursors)
        res
    }
}

fun clang_disposeTokens(TU: CXTranslationUnit?, Tokens: CValuesRef<CXToken>?, NumTokens: Int): Unit {
    return memScoped {
        val _TU = TU.rawValue
        val _Tokens = Tokens?.getPointer(memScope).rawValue
        val _NumTokens = NumTokens
        val res = externals.clang_disposeTokens(_TU, _Tokens, _NumTokens)
        res
    }
}

fun clang_getCursorKindSpelling(Kind: CXCursorKind): CValue<CXString> {
    return memScoped {
        val _Kind = Kind.value
        val res = externals.clang_getCursorKindSpelling(_Kind, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getDefinitionSpellingAndExtent(arg0: CValue<CXCursor>, startBuf: CValuesRef<CPointerVar<CInt8Var>>?, endBuf: CValuesRef<CPointerVar<CInt8Var>>?, startLine: CValuesRef<CInt32Var>?, startColumn: CValuesRef<CInt32Var>?, endLine: CValuesRef<CInt32Var>?, endColumn: CValuesRef<CInt32Var>?): Unit {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val _startBuf = startBuf?.getPointer(memScope).rawValue
        val _endBuf = endBuf?.getPointer(memScope).rawValue
        val _startLine = startLine?.getPointer(memScope).rawValue
        val _startColumn = startColumn?.getPointer(memScope).rawValue
        val _endLine = endLine?.getPointer(memScope).rawValue
        val _endColumn = endColumn?.getPointer(memScope).rawValue
        val res = externals.clang_getDefinitionSpellingAndExtent(_arg0, _startBuf, _endBuf, _startLine, _startColumn, _endLine, _endColumn)
        res
    }
}

fun clang_enableStackTraces(): Unit {
    val res = externals.clang_enableStackTraces()
    return res
}

fun clang_executeOnThread(fn: CPointer<CFunction<CFunctionType2>>?, user_data: COpaquePointer?, stack_size: Int): Unit {
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

fun clang_getCompletionChunkText(completion_string: CXCompletionString?, chunk_number: Int): CValue<CXString> {
    return memScoped {
        val _completion_string = completion_string.rawValue
        val _chunk_number = chunk_number
        val res = externals.clang_getCompletionChunkText(_completion_string, _chunk_number, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getCompletionChunkCompletionString(completion_string: CXCompletionString?, chunk_number: Int): CXCompletionString? {
    val _completion_string = completion_string.rawValue
    val _chunk_number = chunk_number
    val res = externals.clang_getCompletionChunkCompletionString(_completion_string, _chunk_number)
    return interpretCPointer<COpaque>(res)
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

fun clang_getCompletionAnnotation(completion_string: CXCompletionString?, annotation_number: Int): CValue<CXString> {
    return memScoped {
        val _completion_string = completion_string.rawValue
        val _annotation_number = annotation_number
        val res = externals.clang_getCompletionAnnotation(_completion_string, _annotation_number, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getCompletionParent(completion_string: CXCompletionString?, kind: CValuesRef<CXCursorKind.Var>?): CValue<CXString> {
    return memScoped {
        val _completion_string = completion_string.rawValue
        val _kind = kind?.getPointer(memScope).rawValue
        val res = externals.clang_getCompletionParent(_completion_string, _kind, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getCompletionBriefComment(completion_string: CXCompletionString?): CValue<CXString> {
    return memScoped {
        val _completion_string = completion_string.rawValue
        val res = externals.clang_getCompletionBriefComment(_completion_string, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getCursorCompletionString(cursor: CValue<CXCursor>): CXCompletionString? {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = externals.clang_getCursorCompletionString(_cursor)
        interpretCPointer<COpaque>(res)
    }
}

fun clang_defaultCodeCompleteOptions(): Int {
    val res = externals.clang_defaultCodeCompleteOptions()
    return res
}

fun clang_codeCompleteAt(TU: CXTranslationUnit?, complete_filename: String?, complete_line: Int, complete_column: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, options: Int): CPointer<CXCodeCompleteResults>? {
    return memScoped {
        val _TU = TU.rawValue
        val _complete_filename = complete_filename?.cstr?.getPointer(memScope).rawValue
        val _complete_line = complete_line
        val _complete_column = complete_column
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val _num_unsaved_files = num_unsaved_files
        val _options = options
        val res = externals.clang_codeCompleteAt(_TU, _complete_filename, _complete_line, _complete_column, _unsaved_files, _num_unsaved_files, _options)
        interpretCPointer<CXCodeCompleteResults>(res)
    }
}

fun clang_sortCodeCompletionResults(Results: CValuesRef<CXCompletionResult>?, NumResults: Int): Unit {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val _NumResults = NumResults
        val res = externals.clang_sortCodeCompletionResults(_Results, _NumResults)
        res
    }
}

fun clang_disposeCodeCompleteResults(Results: CValuesRef<CXCodeCompleteResults>?): Unit {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val res = externals.clang_disposeCodeCompleteResults(_Results)
        res
    }
}

fun clang_codeCompleteGetNumDiagnostics(Results: CValuesRef<CXCodeCompleteResults>?): Int {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val res = externals.clang_codeCompleteGetNumDiagnostics(_Results)
        res
    }
}

fun clang_codeCompleteGetDiagnostic(Results: CValuesRef<CXCodeCompleteResults>?, Index: Int): CXDiagnostic? {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val _Index = Index
        val res = externals.clang_codeCompleteGetDiagnostic(_Results, _Index)
        interpretCPointer<COpaque>(res)
    }
}

fun clang_codeCompleteGetContexts(Results: CValuesRef<CXCodeCompleteResults>?): Long {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val res = externals.clang_codeCompleteGetContexts(_Results)
        res
    }
}

fun clang_codeCompleteGetContainerKind(Results: CValuesRef<CXCodeCompleteResults>?, IsIncomplete: CValuesRef<CInt32Var>?): CXCursorKind {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val _IsIncomplete = IsIncomplete?.getPointer(memScope).rawValue
        val res = externals.clang_codeCompleteGetContainerKind(_Results, _IsIncomplete)
        CXCursorKind.byValue(res)
    }
}

fun clang_codeCompleteGetContainerUSR(Results: CValuesRef<CXCodeCompleteResults>?): CValue<CXString> {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val res = externals.clang_codeCompleteGetContainerUSR(_Results, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_codeCompleteGetObjCSelector(Results: CValuesRef<CXCodeCompleteResults>?): CValue<CXString> {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val res = externals.clang_codeCompleteGetObjCSelector(_Results, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

fun clang_getClangVersion(): CValue<CXString> {
    return memScoped {
        val res = externals.clang_getClangVersion(alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
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

fun clang_Cursor_Evaluate(C: CValue<CXCursor>): CXEvalResult? {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = externals.clang_Cursor_Evaluate(_C)
        interpretCPointer<COpaque>(res)
    }
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
    return interpretCPointer<CInt8Var>(res)
}

fun clang_EvalResult_dispose(E: CXEvalResult?): Unit {
    val _E = E.rawValue
    val res = externals.clang_EvalResult_dispose(_E)
    return res
}

fun clang_getRemappings(path: String?): CXRemapping? {
    return memScoped {
        val _path = path?.cstr?.getPointer(memScope).rawValue
        val res = externals.clang_getRemappings(_path)
        interpretCPointer<COpaque>(res)
    }
}

fun clang_getRemappingsFromFileList(filePaths: CValuesRef<CPointerVar<CInt8Var>>?, numFiles: Int): CXRemapping? {
    return memScoped {
        val _filePaths = filePaths?.getPointer(memScope).rawValue
        val _numFiles = numFiles
        val res = externals.clang_getRemappingsFromFileList(_filePaths, _numFiles)
        interpretCPointer<COpaque>(res)
    }
}

fun clang_remap_getNumFiles(arg0: CXRemapping?): Int {
    val _arg0 = arg0.rawValue
    val res = externals.clang_remap_getNumFiles(_arg0)
    return res
}

fun clang_remap_getFilenames(arg0: CXRemapping?, index: Int, original: CValuesRef<CXString>?, transformed: CValuesRef<CXString>?): Unit {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _index = index
        val _original = original?.getPointer(memScope).rawValue
        val _transformed = transformed?.getPointer(memScope).rawValue
        val res = externals.clang_remap_getFilenames(_arg0, _index, _original, _transformed)
        res
    }
}

fun clang_remap_dispose(arg0: CXRemapping?): Unit {
    val _arg0 = arg0.rawValue
    val res = externals.clang_remap_dispose(_arg0)
    return res
}

fun clang_findReferencesInFile(cursor: CValue<CXCursor>, file: CXFile?, visitor: CValue<CXCursorAndRangeVisitor>): CXResult {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val _file = file.rawValue
        val _visitor = visitor.getPointer(memScope).rawValue
        val res = externals.clang_findReferencesInFile(_cursor, _file, _visitor)
        CXResult.byValue(res)
    }
}

fun clang_findIncludesInFile(TU: CXTranslationUnit?, file: CXFile?, visitor: CValue<CXCursorAndRangeVisitor>): CXResult {
    return memScoped {
        val _TU = TU.rawValue
        val _file = file.rawValue
        val _visitor = visitor.getPointer(memScope).rawValue
        val res = externals.clang_findIncludesInFile(_TU, _file, _visitor)
        CXResult.byValue(res)
    }
}

fun clang_index_isEntityObjCContainerKind(arg0: CXIdxEntityKind): Int {
    val _arg0 = arg0.value
    val res = externals.clang_index_isEntityObjCContainerKind(_arg0)
    return res
}

fun clang_index_getObjCContainerDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCContainerDeclInfo>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.clang_index_getObjCContainerDeclInfo(_arg0)
        interpretCPointer<CXIdxObjCContainerDeclInfo>(res)
    }
}

fun clang_index_getObjCInterfaceDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCInterfaceDeclInfo>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.clang_index_getObjCInterfaceDeclInfo(_arg0)
        interpretCPointer<CXIdxObjCInterfaceDeclInfo>(res)
    }
}

fun clang_index_getObjCCategoryDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCCategoryDeclInfo>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.clang_index_getObjCCategoryDeclInfo(_arg0)
        interpretCPointer<CXIdxObjCCategoryDeclInfo>(res)
    }
}

fun clang_index_getObjCProtocolRefListInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCProtocolRefListInfo>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.clang_index_getObjCProtocolRefListInfo(_arg0)
        interpretCPointer<CXIdxObjCProtocolRefListInfo>(res)
    }
}

fun clang_index_getObjCPropertyDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCPropertyDeclInfo>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.clang_index_getObjCPropertyDeclInfo(_arg0)
        interpretCPointer<CXIdxObjCPropertyDeclInfo>(res)
    }
}

fun clang_index_getIBOutletCollectionAttrInfo(arg0: CValuesRef<CXIdxAttrInfo>?): CPointer<CXIdxIBOutletCollectionAttrInfo>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.clang_index_getIBOutletCollectionAttrInfo(_arg0)
        interpretCPointer<CXIdxIBOutletCollectionAttrInfo>(res)
    }
}

fun clang_index_getCXXClassDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxCXXClassDeclInfo>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.clang_index_getCXXClassDeclInfo(_arg0)
        interpretCPointer<CXIdxCXXClassDeclInfo>(res)
    }
}

fun clang_index_getClientContainer(arg0: CValuesRef<CXIdxContainerInfo>?): CXIdxClientContainer? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.clang_index_getClientContainer(_arg0)
        interpretCPointer<COpaque>(res)
    }
}

fun clang_index_setClientContainer(arg0: CValuesRef<CXIdxContainerInfo>?, arg1: CXIdxClientContainer?): Unit {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val _arg1 = arg1.rawValue
        val res = externals.clang_index_setClientContainer(_arg0, _arg1)
        res
    }
}

fun clang_index_getClientEntity(arg0: CValuesRef<CXIdxEntityInfo>?): CXIdxClientEntity? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = externals.clang_index_getClientEntity(_arg0)
        interpretCPointer<COpaque>(res)
    }
}

fun clang_index_setClientEntity(arg0: CValuesRef<CXIdxEntityInfo>?, arg1: CXIdxClientEntity?): Unit {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val _arg1 = arg1.rawValue
        val res = externals.clang_index_setClientEntity(_arg0, _arg1)
        res
    }
}

fun clang_IndexAction_create(CIdx: CXIndex?): CXIndexAction? {
    val _CIdx = CIdx.rawValue
    val res = externals.clang_IndexAction_create(_CIdx)
    return interpretCPointer<COpaque>(res)
}

fun clang_IndexAction_dispose(arg0: CXIndexAction?): Unit {
    val _arg0 = arg0.rawValue
    val res = externals.clang_IndexAction_dispose(_arg0)
    return res
}

fun clang_indexSourceFile(arg0: CXIndexAction?, client_data: CXClientData?, index_callbacks: CValuesRef<IndexerCallbacks>?, index_callbacks_size: Int, index_options: Int, source_filename: String?, command_line_args: CValuesRef<CPointerVar<CInt8Var>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, out_TU: CValuesRef<CXTranslationUnitVar>?, TU_options: Int): Int {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _client_data = client_data.rawValue
        val _index_callbacks = index_callbacks?.getPointer(memScope).rawValue
        val _index_callbacks_size = index_callbacks_size
        val _index_options = index_options
        val _source_filename = source_filename?.cstr?.getPointer(memScope).rawValue
        val _command_line_args = command_line_args?.getPointer(memScope).rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val _num_unsaved_files = num_unsaved_files
        val _out_TU = out_TU?.getPointer(memScope).rawValue
        val _TU_options = TU_options
        val res = externals.clang_indexSourceFile(_arg0, _client_data, _index_callbacks, _index_callbacks_size, _index_options, _source_filename, _command_line_args, _num_command_line_args, _unsaved_files, _num_unsaved_files, _out_TU, _TU_options)
        res
    }
}

fun clang_indexSourceFileFullArgv(arg0: CXIndexAction?, client_data: CXClientData?, index_callbacks: CValuesRef<IndexerCallbacks>?, index_callbacks_size: Int, index_options: Int, source_filename: String?, command_line_args: CValuesRef<CPointerVar<CInt8Var>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, out_TU: CValuesRef<CXTranslationUnitVar>?, TU_options: Int): Int {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _client_data = client_data.rawValue
        val _index_callbacks = index_callbacks?.getPointer(memScope).rawValue
        val _index_callbacks_size = index_callbacks_size
        val _index_options = index_options
        val _source_filename = source_filename?.cstr?.getPointer(memScope).rawValue
        val _command_line_args = command_line_args?.getPointer(memScope).rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val _num_unsaved_files = num_unsaved_files
        val _out_TU = out_TU?.getPointer(memScope).rawValue
        val _TU_options = TU_options
        val res = externals.clang_indexSourceFileFullArgv(_arg0, _client_data, _index_callbacks, _index_callbacks_size, _index_options, _source_filename, _command_line_args, _num_command_line_args, _unsaved_files, _num_unsaved_files, _out_TU, _TU_options)
        res
    }
}

fun clang_indexTranslationUnit(arg0: CXIndexAction?, client_data: CXClientData?, index_callbacks: CValuesRef<IndexerCallbacks>?, index_callbacks_size: Int, index_options: Int, arg5: CXTranslationUnit?): Int {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _client_data = client_data.rawValue
        val _index_callbacks = index_callbacks?.getPointer(memScope).rawValue
        val _index_callbacks_size = index_callbacks_size
        val _index_options = index_options
        val _arg5 = arg5.rawValue
        val res = externals.clang_indexTranslationUnit(_arg0, _client_data, _index_callbacks, _index_callbacks_size, _index_options, _arg5)
        res
    }
}

fun clang_indexLoc_getFileLocation(loc: CValue<CXIdxLoc>, indexFile: CValuesRef<CXIdxClientFileVar>?, file: CValuesRef<CXFileVar>?, line: CValuesRef<CInt32Var>?, column: CValuesRef<CInt32Var>?, offset: CValuesRef<CInt32Var>?): Unit {
    return memScoped {
        val _loc = loc.getPointer(memScope).rawValue
        val _indexFile = indexFile?.getPointer(memScope).rawValue
        val _file = file?.getPointer(memScope).rawValue
        val _line = line?.getPointer(memScope).rawValue
        val _column = column?.getPointer(memScope).rawValue
        val _offset = offset?.getPointer(memScope).rawValue
        val res = externals.clang_indexLoc_getFileLocation(_loc, _indexFile, _file, _line, _column, _offset)
        res
    }
}

fun clang_indexLoc_getCXSourceLocation(loc: CValue<CXIdxLoc>): CValue<CXSourceLocation> {
    return memScoped {
        val _loc = loc.getPointer(memScope).rawValue
        val res = externals.clang_indexLoc_getCXSourceLocation(_loc, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

fun clang_Type_visitFields(T: CValue<CXType>, visitor: CXFieldVisitor?, client_data: CXClientData?): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val _visitor = visitor.rawValue
        val _client_data = client_data.rawValue
        val res = externals.clang_Type_visitFields(_T, _visitor, _client_data)
        res
    }
}

val __llvm__: Int = 1
val __clang__: Int = 1
val __clang_major__: Int = 3
val __clang_minor__: Int = 9
val __clang_patchlevel__: Int = 0
val __GNUC_MINOR__: Int = 2
val __GNUC_PATCHLEVEL__: Int = 1
val __GNUC__: Int = 4
val __GXX_ABI_VERSION: Int = 1002
val __ATOMIC_RELAXED: Int = 0
val __ATOMIC_CONSUME: Int = 1
val __ATOMIC_ACQUIRE: Int = 2
val __ATOMIC_RELEASE: Int = 3
val __ATOMIC_ACQ_REL: Int = 4
val __ATOMIC_SEQ_CST: Int = 5
val __PRAGMA_REDEFINE_EXTNAME: Int = 1
val __STRICT_ANSI__: Int = 1
val __CONSTANT_CFSTRINGS__: Int = 1
val __BLOCKS__: Int = 1
val __ORDER_LITTLE_ENDIAN__: Int = 1234
val __ORDER_BIG_ENDIAN__: Int = 4321
val __ORDER_PDP_ENDIAN__: Int = 3412
val __BYTE_ORDER__: Int = 1234
val __LITTLE_ENDIAN__: Int = 1
val _LP64: Int = 1
val __LP64__: Int = 1
val __CHAR_BIT__: Int = 8
val __SCHAR_MAX__: Int = 127
val __SHRT_MAX__: Int = 32767
val __INT_MAX__: Int = 2147483647
val __LONG_MAX__: Long = 9223372036854775807
val __LONG_LONG_MAX__: Long = 9223372036854775807
val __WCHAR_MAX__: Int = 2147483647
val __INTMAX_MAX__: Long = 9223372036854775807
val __SIZE_MAX__: Long = -1
val __UINTMAX_MAX__: Long = -1
val __PTRDIFF_MAX__: Long = 9223372036854775807
val __INTPTR_MAX__: Long = 9223372036854775807
val __UINTPTR_MAX__: Long = -1
val __SIZEOF_DOUBLE__: Int = 8
val __SIZEOF_FLOAT__: Int = 4
val __SIZEOF_INT__: Int = 4
val __SIZEOF_LONG__: Int = 8
val __SIZEOF_LONG_DOUBLE__: Int = 16
val __SIZEOF_LONG_LONG__: Int = 8
val __SIZEOF_POINTER__: Int = 8
val __SIZEOF_SHORT__: Int = 2
val __SIZEOF_PTRDIFF_T__: Int = 8
val __SIZEOF_SIZE_T__: Int = 8
val __SIZEOF_WCHAR_T__: Int = 4
val __SIZEOF_WINT_T__: Int = 4
val __SIZEOF_INT128__: Int = 16
val __INTMAX_WIDTH__: Int = 64
val __PTRDIFF_WIDTH__: Int = 64
val __INTPTR_WIDTH__: Int = 64
val __SIZE_WIDTH__: Int = 64
val __WCHAR_WIDTH__: Int = 32
val __WINT_WIDTH__: Int = 32
val __SIG_ATOMIC_WIDTH__: Int = 32
val __SIG_ATOMIC_MAX__: Int = 2147483647
val __UINTMAX_WIDTH__: Int = 64
val __UINTPTR_WIDTH__: Int = 64
val __FLT_DENORM_MIN__: Float = bitsToFloat(1) /* == 1.4E-45 */
val __FLT_HAS_DENORM__: Int = 1
val __FLT_DIG__: Int = 6
val __FLT_DECIMAL_DIG__: Int = 9
val __FLT_EPSILON__: Float = bitsToFloat(872415232) /* == 1.1920929E-7 */
val __FLT_HAS_INFINITY__: Int = 1
val __FLT_HAS_QUIET_NAN__: Int = 1
val __FLT_MANT_DIG__: Int = 24
val __FLT_MAX_10_EXP__: Int = 38
val __FLT_MAX_EXP__: Int = 128
val __FLT_MAX__: Float = bitsToFloat(2139095039) /* == 3.4028235E38 */
val __FLT_MIN_10_EXP__: Int = -37
val __FLT_MIN_EXP__: Int = -125
val __FLT_MIN__: Float = bitsToFloat(8388608) /* == 1.17549435E-38 */
val __DBL_DENORM_MIN__: Double = bitsToDouble(1) /* == 4.9E-324 */
val __DBL_HAS_DENORM__: Int = 1
val __DBL_DIG__: Int = 15
val __DBL_DECIMAL_DIG__: Int = 17
val __DBL_EPSILON__: Double = bitsToDouble(4372995238176751616) /* == 2.220446049250313E-16 */
val __DBL_HAS_INFINITY__: Int = 1
val __DBL_HAS_QUIET_NAN__: Int = 1
val __DBL_MANT_DIG__: Int = 53
val __DBL_MAX_10_EXP__: Int = 308
val __DBL_MAX_EXP__: Int = 1024
val __DBL_MAX__: Double = bitsToDouble(9218868437227405311) /* == 1.7976931348623157E308 */
val __DBL_MIN_10_EXP__: Int = -307
val __DBL_MIN_EXP__: Int = -1021
val __DBL_MIN__: Double = bitsToDouble(4503599627370496) /* == 2.2250738585072014E-308 */
val __LDBL_HAS_DENORM__: Int = 1
val __LDBL_DIG__: Int = 18
val __LDBL_DECIMAL_DIG__: Int = 21
val __LDBL_HAS_INFINITY__: Int = 1
val __LDBL_HAS_QUIET_NAN__: Int = 1
val __LDBL_MANT_DIG__: Int = 64
val __LDBL_MAX_10_EXP__: Int = 4932
val __LDBL_MAX_EXP__: Int = 16384
val __LDBL_MIN_10_EXP__: Int = -4931
val __LDBL_MIN_EXP__: Int = -16381
val __POINTER_WIDTH__: Int = 64
val __BIGGEST_ALIGNMENT__: Int = 16
val __UINT8_MAX__: Int = 255
val __INT8_MAX__: Int = 127
val __UINT16_MAX__: Int = 65535
val __INT16_MAX__: Int = 32767
val __UINT32_MAX__: Int = -1
val __INT32_MAX__: Int = 2147483647
val __UINT64_MAX__: Long = -1
val __INT64_MAX__: Long = 9223372036854775807
val __INT_LEAST8_MAX__: Int = 127
val __UINT_LEAST8_MAX__: Int = 255
val __INT_LEAST16_MAX__: Int = 32767
val __UINT_LEAST16_MAX__: Int = 65535
val __INT_LEAST32_MAX__: Int = 2147483647
val __UINT_LEAST32_MAX__: Int = -1
val __INT_LEAST64_MAX__: Long = 9223372036854775807
val __UINT_LEAST64_MAX__: Long = -1
val __INT_FAST8_MAX__: Int = 127
val __UINT_FAST8_MAX__: Int = 255
val __INT_FAST16_MAX__: Int = 32767
val __UINT_FAST16_MAX__: Int = 65535
val __INT_FAST32_MAX__: Int = 2147483647
val __UINT_FAST32_MAX__: Int = -1
val __INT_FAST64_MAX__: Long = 9223372036854775807
val __UINT_FAST64_MAX__: Long = -1
val __FINITE_MATH_ONLY__: Int = 0
val __GNUC_STDC_INLINE__: Int = 1
val __GCC_ATOMIC_TEST_AND_SET_TRUEVAL: Int = 1
val __GCC_ATOMIC_BOOL_LOCK_FREE: Int = 2
val __GCC_ATOMIC_CHAR_LOCK_FREE: Int = 2
val __GCC_ATOMIC_CHAR16_T_LOCK_FREE: Int = 2
val __GCC_ATOMIC_CHAR32_T_LOCK_FREE: Int = 2
val __GCC_ATOMIC_WCHAR_T_LOCK_FREE: Int = 2
val __GCC_ATOMIC_SHORT_LOCK_FREE: Int = 2
val __GCC_ATOMIC_INT_LOCK_FREE: Int = 2
val __GCC_ATOMIC_LONG_LOCK_FREE: Int = 2
val __GCC_ATOMIC_LLONG_LOCK_FREE: Int = 2
val __GCC_ATOMIC_POINTER_LOCK_FREE: Int = 2
val __NO_INLINE__: Int = 1
val __PIC__: Int = 2
val __pic__: Int = 2
val __FLT_EVAL_METHOD__: Int = 0
val __FLT_RADIX__: Int = 2
val __DECIMAL_DIG__: Int = 21
val __SSP__: Int = 1
val __amd64__: Int = 1
val __amd64: Int = 1
val __x86_64: Int = 1
val __x86_64__: Int = 1
val __core2: Int = 1
val __core2__: Int = 1
val __tune_core2__: Int = 1
val __NO_MATH_INLINES: Int = 1
val __FXSR__: Int = 1
val __GCC_HAVE_SYNC_COMPARE_AND_SWAP_16: Int = 1
val __SSSE3__: Int = 1
val __SSE3__: Int = 1
val __SSE2__: Int = 1
val __SSE2_MATH__: Int = 1
val __SSE__: Int = 1
val __SSE_MATH__: Int = 1
val __MMX__: Int = 1
val __GCC_HAVE_SYNC_COMPARE_AND_SWAP_1: Int = 1
val __GCC_HAVE_SYNC_COMPARE_AND_SWAP_2: Int = 1
val __GCC_HAVE_SYNC_COMPARE_AND_SWAP_4: Int = 1
val __GCC_HAVE_SYNC_COMPARE_AND_SWAP_8: Int = 1
val __APPLE_CC__: Int = 6000
val __APPLE__: Int = 1
val OBJC_NEW_PROPERTIES: Int = 1
val __DYNAMIC__: Int = 1
val __ENVIRONMENT_MAC_OS_X_VERSION_MIN_REQUIRED__: Int = 101000
val __MACH__: Int = 1
val __STDC__: Int = 1
val __STDC_HOSTED__: Int = 1
val __STDC_VERSION__: Long = 199901
val __STDC_UTF_16__: Int = 1
val __STDC_UTF_32__: Int = 1
val __DARWIN_ONLY_64_BIT_INO_T: Int = 0
val __DARWIN_ONLY_VERS_1050: Int = 0
val __DARWIN_ONLY_UNIX_CONFORMANCE: Int = 1
val __DARWIN_UNIX03: Int = 1
val __DARWIN_64_BIT_INO_T: Int = 1
val __DARWIN_VERS_1050: Int = 1
val __DARWIN_NON_CANCELABLE: Int = 0
val __DARWIN_C_ANSI: Long = 4096
val __DARWIN_C_FULL: Long = 900000
val __DARWIN_C_LEVEL: Long = 900000
val _DARWIN_FEATURE_64_BIT_INODE: Int = 1
val _DARWIN_FEATURE_ONLY_UNIX_CONFORMANCE: Int = 1
val _DARWIN_FEATURE_UNIX_CONFORMANCE: Int = 3
val __PTHREAD_SIZE__: Int = 8176
val __PTHREAD_ATTR_SIZE__: Int = 56
val __PTHREAD_MUTEXATTR_SIZE__: Int = 8
val __PTHREAD_MUTEX_SIZE__: Int = 56
val __PTHREAD_CONDATTR_SIZE__: Int = 8
val __PTHREAD_COND_SIZE__: Int = 40
val __PTHREAD_ONCE_SIZE__: Int = 8
val __PTHREAD_RWLOCK_SIZE__: Int = 192
val __PTHREAD_RWLOCKATTR_SIZE__: Int = 16
val __DARWIN_WCHAR_MAX: Int = 2147483647
val __DARWIN_WCHAR_MIN: Int = -2147483648
val __DARWIN_WEOF: __darwin_wint_t = -1
val _FORTIFY_SOURCE: Int = 2
val __MAC_10_0: Int = 1000
val __MAC_10_1: Int = 1010
val __MAC_10_2: Int = 1020
val __MAC_10_3: Int = 1030
val __MAC_10_4: Int = 1040
val __MAC_10_5: Int = 1050
val __MAC_10_6: Int = 1060
val __MAC_10_7: Int = 1070
val __MAC_10_8: Int = 1080
val __MAC_10_9: Int = 1090
val __MAC_10_10: Int = 101000
val __MAC_10_10_2: Int = 101002
val __MAC_10_10_3: Int = 101003
val __MAC_10_11: Int = 101100
val __MAC_10_11_2: Int = 101102
val __MAC_10_11_3: Int = 101103
val __MAC_10_11_4: Int = 101104
val __MAC_10_12: Int = 101200
val __IPHONE_2_0: Int = 20000
val __IPHONE_2_1: Int = 20100
val __IPHONE_2_2: Int = 20200
val __IPHONE_3_0: Int = 30000
val __IPHONE_3_1: Int = 30100
val __IPHONE_3_2: Int = 30200
val __IPHONE_4_0: Int = 40000
val __IPHONE_4_1: Int = 40100
val __IPHONE_4_2: Int = 40200
val __IPHONE_4_3: Int = 40300
val __IPHONE_5_0: Int = 50000
val __IPHONE_5_1: Int = 50100
val __IPHONE_6_0: Int = 60000
val __IPHONE_6_1: Int = 60100
val __IPHONE_7_0: Int = 70000
val __IPHONE_7_1: Int = 70100
val __IPHONE_8_0: Int = 80000
val __IPHONE_8_1: Int = 80100
val __IPHONE_8_2: Int = 80200
val __IPHONE_8_3: Int = 80300
val __IPHONE_8_4: Int = 80400
val __IPHONE_9_0: Int = 90000
val __IPHONE_9_1: Int = 90100
val __IPHONE_9_2: Int = 90200
val __IPHONE_9_3: Int = 90300
val __IPHONE_10_0: Int = 100000
val __TVOS_9_0: Int = 90000
val __TVOS_9_1: Int = 90100
val __TVOS_9_2: Int = 90200
val __TVOS_10_0: Int = 100000
val __WATCHOS_1_0: Int = 10000
val __WATCHOS_2_0: Int = 20000
val __WATCHOS_3_0: Int = 30000
val __MAC_OS_X_VERSION_MIN_REQUIRED: Int = 101000
val __MAC_OS_X_VERSION_MAX_ALLOWED: Int = 101200
val CLOCKS_PER_SEC: Int = 1000000
val CLOCK_REALTIME: Int = 0
val CLOCK_MONOTONIC: Int = 6
val CLOCK_MONOTONIC_RAW: Int = 4
val CLOCK_MONOTONIC_RAW_APPROX: Int = 5
val CLOCK_UPTIME_RAW: Int = 8
val CLOCK_UPTIME_RAW_APPROX: Int = 9
val CLOCK_PROCESS_CPUTIME_ID: Int = 12
val CLOCK_THREAD_CPUTIME_ID: Int = 16
val CINDEX_VERSION_MAJOR: Int = 0
val CINDEX_VERSION_MINOR: Int = 35
val CINDEX_VERSION: Int = 35

class __mbstate_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(128, 8)
    
    val __mbstate8: CArray<CInt8Var>
        get() = memberAt(0)
    
    val _mbstateL: CInt64Var
        get() = memberAt(0)
    
}

class __darwin_pthread_handler_rec(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    val __routine: CPointerVar<CFunction<CFunctionType5>>
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
    
    val visit: CPointerVar<CFunction<CFunctionType6>>
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
    
    val kind: CXIdxAttrKindVar
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
    
    val templateKind: CXIdxEntityCXXTemplateKindVar
        get() = memberAt(4)
    
    val lang: CXIdxEntityLanguageVar
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
    
    val kind: CXIdxObjCContainerKindVar
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
    
    val kind: CXIdxEntityRefKindVar
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
    
    val abortQuery: CPointerVar<CFunction<CFunctionType7>>
        get() = memberAt(0)
    
    val diagnostic: CPointerVar<CFunction<CFunctionType8>>
        get() = memberAt(8)
    
    val enteredMainFile: CPointerVar<CFunction<CFunctionType9>>
        get() = memberAt(16)
    
    val ppIncludedFile: CPointerVar<CFunction<CFunctionType10>>
        get() = memberAt(24)
    
    val importedASTFile: CPointerVar<CFunction<CFunctionType11>>
        get() = memberAt(32)
    
    val startedTranslationUnit: CPointerVar<CFunction<CFunctionType12>>
        get() = memberAt(40)
    
    val indexDeclaration: CPointerVar<CFunction<CFunctionType13>>
        get() = memberAt(48)
    
    val indexEntityReference: CPointerVar<CFunction<CFunctionType14>>
        get() = memberAt(56)
    
}

typealias clockid_tVar = CInt32VarWithValueMappedTo<clockid_t>
typealias clockid_t = Int

val _CLOCK_REALTIME: clockid_t = 0
val _CLOCK_MONOTONIC: clockid_t = 6
val _CLOCK_MONOTONIC_RAW: clockid_t = 4
val _CLOCK_MONOTONIC_RAW_APPROX: clockid_t = 5
val _CLOCK_UPTIME_RAW: clockid_t = 8
val _CLOCK_UPTIME_RAW_APPROX: clockid_t = 9
val _CLOCK_PROCESS_CPUTIME_ID: clockid_t = 12
val _CLOCK_THREAD_CPUTIME_ID: clockid_t = 16

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

typealias CXGlobalOptFlagsVar = CInt32VarWithValueMappedTo<CXGlobalOptFlags>
typealias CXGlobalOptFlags = Int

val CXGlobalOpt_None: CXGlobalOptFlags = 0
val CXGlobalOpt_ThreadBackgroundPriorityForIndexing: CXGlobalOptFlags = 1
val CXGlobalOpt_ThreadBackgroundPriorityForEditing: CXGlobalOptFlags = 2
val CXGlobalOpt_ThreadBackgroundPriorityForAll: CXGlobalOptFlags = 3

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

typealias CXDiagnosticDisplayOptionsVar = CInt32VarWithValueMappedTo<CXDiagnosticDisplayOptions>
typealias CXDiagnosticDisplayOptions = Int

val CXDiagnostic_DisplaySourceLocation: CXDiagnosticDisplayOptions = 1
val CXDiagnostic_DisplayColumn: CXDiagnosticDisplayOptions = 2
val CXDiagnostic_DisplaySourceRanges: CXDiagnosticDisplayOptions = 4
val CXDiagnostic_DisplayOption: CXDiagnosticDisplayOptions = 8
val CXDiagnostic_DisplayCategoryId: CXDiagnosticDisplayOptions = 16
val CXDiagnostic_DisplayCategoryName: CXDiagnosticDisplayOptions = 32

typealias CXTranslationUnit_FlagsVar = CInt32VarWithValueMappedTo<CXTranslationUnit_Flags>
typealias CXTranslationUnit_Flags = Int

val CXTranslationUnit_None: CXTranslationUnit_Flags = 0
val CXTranslationUnit_DetailedPreprocessingRecord: CXTranslationUnit_Flags = 1
val CXTranslationUnit_Incomplete: CXTranslationUnit_Flags = 2
val CXTranslationUnit_PrecompiledPreamble: CXTranslationUnit_Flags = 4
val CXTranslationUnit_CacheCompletionResults: CXTranslationUnit_Flags = 8
val CXTranslationUnit_ForSerialization: CXTranslationUnit_Flags = 16
val CXTranslationUnit_CXXChainedPCH: CXTranslationUnit_Flags = 32
val CXTranslationUnit_SkipFunctionBodies: CXTranslationUnit_Flags = 64
val CXTranslationUnit_IncludeBriefCommentsInCodeCompletion: CXTranslationUnit_Flags = 128
val CXTranslationUnit_CreatePreambleOnFirstParse: CXTranslationUnit_Flags = 256
val CXTranslationUnit_KeepGoing: CXTranslationUnit_Flags = 512

typealias CXSaveTranslationUnit_FlagsVar = CInt32VarWithValueMappedTo<CXSaveTranslationUnit_Flags>
typealias CXSaveTranslationUnit_Flags = Int

val CXSaveTranslationUnit_None: CXSaveTranslationUnit_Flags = 0

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

typealias CXReparse_FlagsVar = CInt32VarWithValueMappedTo<CXReparse_Flags>
typealias CXReparse_Flags = Int

val CXReparse_None: CXReparse_Flags = 0

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

typealias CXTypeLayoutErrorVar = CInt32VarWithValueMappedTo<CXTypeLayoutError>
typealias CXTypeLayoutError = Int

val CXTypeLayoutError_Invalid: CXTypeLayoutError = -1
val CXTypeLayoutError_Incomplete: CXTypeLayoutError = -2
val CXTypeLayoutError_Dependent: CXTypeLayoutError = -3
val CXTypeLayoutError_NotConstantSize: CXTypeLayoutError = -4
val CXTypeLayoutError_InvalidFieldName: CXTypeLayoutError = -5

typealias CXRefQualifierKindVar = CInt32VarWithValueMappedTo<CXRefQualifierKind>
typealias CXRefQualifierKind = Int

val CXRefQualifier_None: CXRefQualifierKind = 0
val CXRefQualifier_LValue: CXRefQualifierKind = 1
val CXRefQualifier_RValue: CXRefQualifierKind = 2

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

typealias CXObjCPropertyAttrKindVar = CInt32VarWithValueMappedTo<CXObjCPropertyAttrKind>
typealias CXObjCPropertyAttrKind = Int

val CXObjCPropertyAttr_noattr: CXObjCPropertyAttrKind = 0
val CXObjCPropertyAttr_readonly: CXObjCPropertyAttrKind = 1
val CXObjCPropertyAttr_getter: CXObjCPropertyAttrKind = 2
val CXObjCPropertyAttr_assign: CXObjCPropertyAttrKind = 4
val CXObjCPropertyAttr_readwrite: CXObjCPropertyAttrKind = 8
val CXObjCPropertyAttr_retain: CXObjCPropertyAttrKind = 16
val CXObjCPropertyAttr_copy: CXObjCPropertyAttrKind = 32
val CXObjCPropertyAttr_nonatomic: CXObjCPropertyAttrKind = 64
val CXObjCPropertyAttr_setter: CXObjCPropertyAttrKind = 128
val CXObjCPropertyAttr_atomic: CXObjCPropertyAttrKind = 256
val CXObjCPropertyAttr_weak: CXObjCPropertyAttrKind = 512
val CXObjCPropertyAttr_strong: CXObjCPropertyAttrKind = 1024
val CXObjCPropertyAttr_unsafe_unretained: CXObjCPropertyAttrKind = 2048
val CXObjCPropertyAttr_class: CXObjCPropertyAttrKind = 4096

typealias CXObjCDeclQualifierKindVar = CInt32VarWithValueMappedTo<CXObjCDeclQualifierKind>
typealias CXObjCDeclQualifierKind = Int

val CXObjCDeclQualifier_None: CXObjCDeclQualifierKind = 0
val CXObjCDeclQualifier_In: CXObjCDeclQualifierKind = 1
val CXObjCDeclQualifier_Inout: CXObjCDeclQualifierKind = 2
val CXObjCDeclQualifier_Out: CXObjCDeclQualifierKind = 4
val CXObjCDeclQualifier_Bycopy: CXObjCDeclQualifierKind = 8
val CXObjCDeclQualifier_Byref: CXObjCDeclQualifierKind = 16
val CXObjCDeclQualifier_Oneway: CXObjCDeclQualifierKind = 32

typealias CXNameRefFlagsVar = CInt32VarWithValueMappedTo<CXNameRefFlags>
typealias CXNameRefFlags = Int

val CXNameRange_WantQualifier: CXNameRefFlags = 1
val CXNameRange_WantTemplateArgs: CXNameRefFlags = 2
val CXNameRange_WantSinglePiece: CXNameRefFlags = 4

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

typealias CXCodeComplete_FlagsVar = CInt32VarWithValueMappedTo<CXCodeComplete_Flags>
typealias CXCodeComplete_Flags = Int

val CXCodeComplete_IncludeMacros: CXCodeComplete_Flags = 1
val CXCodeComplete_IncludeCodePatterns: CXCodeComplete_Flags = 2
val CXCodeComplete_IncludeBriefComments: CXCodeComplete_Flags = 4

typealias CXCompletionContextVar = CInt32VarWithValueMappedTo<CXCompletionContext>
typealias CXCompletionContext = Int

val CXCompletionContext_Unexposed: CXCompletionContext = 0
val CXCompletionContext_AnyType: CXCompletionContext = 1
val CXCompletionContext_AnyValue: CXCompletionContext = 2
val CXCompletionContext_ObjCObjectValue: CXCompletionContext = 4
val CXCompletionContext_ObjCSelectorValue: CXCompletionContext = 8
val CXCompletionContext_CXXClassTypeValue: CXCompletionContext = 16
val CXCompletionContext_DotMemberAccess: CXCompletionContext = 32
val CXCompletionContext_ArrowMemberAccess: CXCompletionContext = 64
val CXCompletionContext_ObjCPropertyAccess: CXCompletionContext = 128
val CXCompletionContext_EnumTag: CXCompletionContext = 256
val CXCompletionContext_UnionTag: CXCompletionContext = 512
val CXCompletionContext_StructTag: CXCompletionContext = 1024
val CXCompletionContext_ClassTag: CXCompletionContext = 2048
val CXCompletionContext_Namespace: CXCompletionContext = 4096
val CXCompletionContext_NestedNameSpecifier: CXCompletionContext = 8192
val CXCompletionContext_ObjCInterface: CXCompletionContext = 16384
val CXCompletionContext_ObjCProtocol: CXCompletionContext = 32768
val CXCompletionContext_ObjCCategory: CXCompletionContext = 65536
val CXCompletionContext_ObjCInstanceMessage: CXCompletionContext = 131072
val CXCompletionContext_ObjCClassMessage: CXCompletionContext = 262144
val CXCompletionContext_ObjCSelectorName: CXCompletionContext = 524288
val CXCompletionContext_MacroName: CXCompletionContext = 1048576
val CXCompletionContext_NaturalLanguage: CXCompletionContext = 2097152
val CXCompletionContext_Unknown: CXCompletionContext = 4194303

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

typealias CXIdxEntityLanguageVar = CInt32VarWithValueMappedTo<CXIdxEntityLanguage>
typealias CXIdxEntityLanguage = Int

val CXIdxEntityLang_None: CXIdxEntityLanguage = 0
val CXIdxEntityLang_C: CXIdxEntityLanguage = 1
val CXIdxEntityLang_ObjC: CXIdxEntityLanguage = 2
val CXIdxEntityLang_CXX: CXIdxEntityLanguage = 3

typealias CXIdxEntityCXXTemplateKindVar = CInt32VarWithValueMappedTo<CXIdxEntityCXXTemplateKind>
typealias CXIdxEntityCXXTemplateKind = Int

val CXIdxEntity_NonTemplate: CXIdxEntityCXXTemplateKind = 0
val CXIdxEntity_Template: CXIdxEntityCXXTemplateKind = 1
val CXIdxEntity_TemplatePartialSpecialization: CXIdxEntityCXXTemplateKind = 2
val CXIdxEntity_TemplateSpecialization: CXIdxEntityCXXTemplateKind = 3

typealias CXIdxAttrKindVar = CInt32VarWithValueMappedTo<CXIdxAttrKind>
typealias CXIdxAttrKind = Int

val CXIdxAttr_Unexposed: CXIdxAttrKind = 0
val CXIdxAttr_IBAction: CXIdxAttrKind = 1
val CXIdxAttr_IBOutlet: CXIdxAttrKind = 2
val CXIdxAttr_IBOutletCollection: CXIdxAttrKind = 3

typealias CXIdxDeclInfoFlagsVar = CInt32VarWithValueMappedTo<CXIdxDeclInfoFlags>
typealias CXIdxDeclInfoFlags = Int

val CXIdxDeclFlag_Skipped: CXIdxDeclInfoFlags = 1

typealias CXIdxObjCContainerKindVar = CInt32VarWithValueMappedTo<CXIdxObjCContainerKind>
typealias CXIdxObjCContainerKind = Int

val CXIdxObjCContainer_ForwardRef: CXIdxObjCContainerKind = 0
val CXIdxObjCContainer_Interface: CXIdxObjCContainerKind = 1
val CXIdxObjCContainer_Implementation: CXIdxObjCContainerKind = 2

typealias CXIdxEntityRefKindVar = CInt32VarWithValueMappedTo<CXIdxEntityRefKind>
typealias CXIdxEntityRefKind = Int

val CXIdxEntityRef_Direct: CXIdxEntityRefKind = 1
val CXIdxEntityRef_Implicit: CXIdxEntityRefKind = 2

typealias CXIndexOptFlagsVar = CInt32VarWithValueMappedTo<CXIndexOptFlags>
typealias CXIndexOptFlags = Int

val CXIndexOpt_None: CXIndexOptFlags = 0
val CXIndexOpt_SuppressRedundantRefs: CXIndexOptFlags = 1
val CXIndexOpt_IndexFunctionLocalSymbols: CXIndexOptFlags = 2
val CXIndexOpt_IndexImplicitTemplateInstantiations: CXIndexOptFlags = 4
val CXIndexOpt_SuppressWarnings: CXIndexOptFlags = 8
val CXIndexOpt_SkipParsedBodiesInSession: CXIndexOptFlags = 16

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
typealias CXCursorVisitor = CPointer<CFunction<CFunctionType1>>

typealias CXClientDataVar = CPointerVarWithValueMappedTo<CXClientData>
typealias CXClientData = COpaquePointer

typealias CXModuleVar = CPointerVarWithValueMappedTo<CXModule>
typealias CXModule = COpaquePointer

typealias CXCompletionStringVar = CPointerVarWithValueMappedTo<CXCompletionString>
typealias CXCompletionString = COpaquePointer

typealias CXInclusionVisitorVar = CPointerVarWithValueMappedTo<CXInclusionVisitor>
typealias CXInclusionVisitor = CPointer<CFunction<CFunctionType3>>

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
typealias CXFieldVisitor = CPointer<CFunction<CFunctionType4>>

typealias __darwin_wint_tVar = CInt32VarWithValueMappedTo<__darwin_wint_t>
typealias __darwin_wint_t = Int

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

object CFunctionType5 : CAdaptedFunctionTypeImpl<(COpaquePointer?) -> Unit>(Void, Pointer) {
    override fun invoke(function: (COpaquePointer?) -> Unit,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value)
    }
}

object CFunctionType6 : CAdaptedFunctionTypeImpl<(COpaquePointer?, CXCursor, CXSourceRange) -> CXVisitorResult>(UInt32, Pointer, Struct(UInt32, SInt32, Struct(Pointer, Pointer, Pointer)), Struct(Struct(Pointer, Pointer), UInt32, UInt32)) {
    override fun invoke(function: (COpaquePointer?, CXCursor, CXSourceRange) -> CXVisitorResult,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<CXCursor>().pointed, args[2].value!!.reinterpret<CXSourceRange>().pointed)
        ret.reinterpret<CXVisitorResult.Var>().pointed.value = res
    }
}

object CFunctionType7 : CAdaptedFunctionTypeImpl<(COpaquePointer?, COpaquePointer?) -> Int>(SInt32, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, COpaquePointer?) -> Int,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<COpaquePointerVar>().pointed.value)
        ret.reinterpret<CInt32Var>().pointed.value = res
    }
}

object CFunctionType8 : CAdaptedFunctionTypeImpl<(COpaquePointer?, COpaquePointer?, COpaquePointer?) -> Unit>(Void, Pointer, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, COpaquePointer?, COpaquePointer?) -> Unit,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[2].value!!.reinterpret<COpaquePointerVar>().pointed.value)
    }
}

object CFunctionType9 : CAdaptedFunctionTypeImpl<(COpaquePointer?, COpaquePointer?, COpaquePointer?) -> COpaquePointer?>(Pointer, Pointer, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, COpaquePointer?, COpaquePointer?) -> COpaquePointer?,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[2].value!!.reinterpret<COpaquePointerVar>().pointed.value)
        ret.reinterpret<COpaquePointerVar>().pointed.value = res
    }
}

object CFunctionType10 : CAdaptedFunctionTypeImpl<(COpaquePointer?, CPointer<CXIdxIncludedFileInfo>?) -> COpaquePointer?>(Pointer, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, CPointer<CXIdxIncludedFileInfo>?) -> COpaquePointer?,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<CPointerVar<CXIdxIncludedFileInfo>>().pointed.value)
        ret.reinterpret<COpaquePointerVar>().pointed.value = res
    }
}

object CFunctionType11 : CAdaptedFunctionTypeImpl<(COpaquePointer?, CPointer<CXIdxImportedASTFileInfo>?) -> COpaquePointer?>(Pointer, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, CPointer<CXIdxImportedASTFileInfo>?) -> COpaquePointer?,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<CPointerVar<CXIdxImportedASTFileInfo>>().pointed.value)
        ret.reinterpret<COpaquePointerVar>().pointed.value = res
    }
}

object CFunctionType12 : CAdaptedFunctionTypeImpl<(COpaquePointer?, COpaquePointer?) -> COpaquePointer?>(Pointer, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, COpaquePointer?) -> COpaquePointer?,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<COpaquePointerVar>().pointed.value)
        ret.reinterpret<COpaquePointerVar>().pointed.value = res
    }
}

object CFunctionType13 : CAdaptedFunctionTypeImpl<(COpaquePointer?, CPointer<CXIdxDeclInfo>?) -> Unit>(Void, Pointer, Pointer) {
    override fun invoke(function: (COpaquePointer?, CPointer<CXIdxDeclInfo>?) -> Unit,  args: CArray<COpaquePointerVar>, ret: COpaquePointer) {
        val res = function(args[0].value!!.reinterpret<COpaquePointerVar>().pointed.value, args[1].value!!.reinterpret<CPointerVar<CXIdxDeclInfo>>().pointed.value)
    }
}

object CFunctionType14 : CAdaptedFunctionTypeImpl<(COpaquePointer?, CPointer<CXIdxEntityRefInfo>?) -> Unit>(Void, Pointer, Pointer) {
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
