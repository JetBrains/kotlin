package org.jetbrains.kotlin.native.test.debugger

import java.io.File
import java.io.Reader
import java.lang.StringBuilder

sealed class DwarfTag(val tag: Tag) {
    enum class Tag(val value: Int) {
        DW_TAG_array_type(0x0001),
        DW_TAG_class_type(0x0002),
        DW_TAG_entry_point(0x0003),
        DW_TAG_enumeration_type(0x0004),
        DW_TAG_formal_parameter(0x0005),
        DW_TAG_imported_declaration(0x0008),
        DW_TAG_label(0x000a),
        DW_TAG_lexical_block(0x000b),
        DW_TAG_member(0x000d),
        DW_TAG_pointer_type(0x000f),
        DW_TAG_reference_type(0x0010),
        DW_TAG_compile_unit(0x0011),
        DW_TAG_string_type(0x0012),
        DW_TAG_structure_type(0x0013),
        DW_TAG_subroutine_type(0x0015),
        DW_TAG_typedef(0x0016),
        DW_TAG_union_type(0x0017),
        DW_TAG_unspecified_parameters(0x0018),
        DW_TAG_variant(0x0019),
        DW_TAG_common_block(0x001a),
        DW_TAG_common_inclusion(0x001b),
        DW_TAG_inheritance(0x001c),
        DW_TAG_inlined_subroutine(0x001d),
        DW_TAG_module(0x001e),
        DW_TAG_ptr_to_member_type(0x001f),
        DW_TAG_set_type(0x0020),
        DW_TAG_subrange_type(0x0021),
        DW_TAG_with_stmt(0x0022),
        DW_TAG_access_declaration(0x0023),
        DW_TAG_base_type(0x0024),
        DW_TAG_catch_block(0x0025),
        DW_TAG_const_type(0x0026),
        DW_TAG_constant(0x0027),
        DW_TAG_enumerator(0x0028),
        DW_TAG_file_type(0x0029),
        DW_TAG_friend(0x002a),
        DW_TAG_namelist(0x002b),
        DW_TAG_namelist_item(0x002c),
        DW_TAG_packed_type(0x002d),
        DW_TAG_subprogram(0x002e),
        DW_TAG_template_type_parameter(0x002f),
        DW_TAG_template_value_parameter(0x0030),
        DW_TAG_thrown_type(0x0031),
        DW_TAG_try_block(0x0032),
        DW_TAG_variant_part(0x0033),
        DW_TAG_variable(0x0034),
        DW_TAG_volatile_type(0x0035),
        DW_TAG_dwarf_procedure(0x0036),
        DW_TAG_restrict_type(0x0037),
        DW_TAG_interface_type(0x0038),
        DW_TAG_namespace(0x0039),
        DW_TAG_imported_module(0x003a),
        DW_TAG_unspecified_type(0x003b),
        DW_TAG_partial_unit(0x003c),
        DW_TAG_imported_unit(0x003d),
        DW_TAG_condition(0x003f),
        DW_TAG_shared_type(0x0040),
        DW_TAG_type_unit(0x0041),
        DW_TAG_rvalue_reference_type(0x0042),
        DW_TAG_template_alias(0x0043),
        DW_TAG_coarray_type(0x0044),
        DW_TAG_generic_subrange(0x0045),
        DW_TAG_dynamic_type(0x0046),
        DW_TAG_MIPS_loop(0x4081),
        DW_TAG_format_label(0x4101),
        DW_TAG_function_template(0x4102),
        DW_TAG_class_template(0x4103),
        DW_TAG_GNU_template_template_param(0x4106),
        DW_TAG_GNU_template_parameter_pack(0x4107),
        DW_TAG_GNU_formal_parameter_pack(0x4108),
        DW_TAG_APPLE_property(0x4200),
        DW_TAG_BORLAND_property(0xb000),
        DW_TAG_BORLAND_Delphi_string(0xb001),
        DW_TAG_BORLAND_Delphi_dynamic_array(0xb002),
        DW_TAG_BORLAND_Delphi_set(0xb003),
        DW_TAG_BORLAND_Delphi_variant(0xb004),
    }
    val attributes = mutableMapOf<DwarfAttribute.Attribute, DwarfAttribute>()
    operator fun DwarfAttribute.unaryPlus() { attributes[this.attribute] = this }
    companion object {
        fun by(name: String)  = by(Tag.valueOf(name))
        fun by(tag: Tag) = when(tag) {
            Tag.DW_TAG_subprogram -> DwarfTagSubprogram()
            else -> DwarfTagDefault(tag)
        }
    }
}

class DwarfTagDefault(tag: DwarfTag.Tag): DwarfTag(tag)
class DwarfTagSubprogram: DwarfTag(Tag.DW_TAG_subprogram) {
    val name: String
        get() = attributes[DwarfAttribute.Attribute.DW_AT_name]!!.rvString
    val path: String?
        get() = attributes[DwarfAttribute.Attribute.DW_AT_decl_file]?.rvString
    val file: File?
        get() = path?.run {  File(this) }
}

class DwarfAttribute(val attribute: Attribute, val rawValue: String) {
    enum class Attribute(val value: Int) {
        DW_AT_sibling(0x01),
        DW_AT_location(0x02),
        DW_AT_name(0x03),
        DW_AT_ordering(0x09),
        DW_AT_byte_size(0x0b),
        DW_AT_bit_offset(0x0c),
        DW_AT_bit_size(0x0d),
        DW_AT_stmt_list(0x10),
        DW_AT_low_pc(0x11),
        DW_AT_high_pc(0x12),
        DW_AT_language(0x13),
        DW_AT_discr(0x15),
        DW_AT_discr_value(0x16),
        DW_AT_visibility(0x17),
        DW_AT_import(0x18),
        DW_AT_string_length(0x19),
        DW_AT_common_reference(0x1a),
        DW_AT_comp_dir(0x1b),
        DW_AT_const_value(0x1c),
        DW_AT_containing_type(0x1d),
        DW_AT_default_value(0x1e),
        DW_AT_inline(0x20),
        DW_AT_is_optional(0x21),
        DW_AT_lower_bound(0x22),
        DW_AT_producer(0x25),
        DW_AT_prototyped(0x27),
        DW_AT_return_addr(0x2a),
        DW_AT_start_scope(0x2c),
        DW_AT_bit_stride(0x2e),
        DW_AT_upper_bound(0x2f),
        DW_AT_abstract_origin(0x31),
        DW_AT_accessibility(0x32),
        DW_AT_address_class(0x33),
        DW_AT_artificial(0x34),
        DW_AT_base_types(0x35),
        DW_AT_calling_convention(0x36),
        DW_AT_count(0x37),
        DW_AT_data_member_location(0x38),
        DW_AT_decl_column(0x39),
        DW_AT_decl_file(0x3a),
        DW_AT_decl_line(0x3b),
        DW_AT_declaration(0x3c),
        DW_AT_discr_list(0x3d),
        DW_AT_encoding(0x3e),
        DW_AT_external(0x3f),
        DW_AT_frame_base(0x40),
        DW_AT_friend(0x41),
        DW_AT_identifier_case(0x42),
        DW_AT_macro_info(0x43),
        DW_AT_namelist_item(0x44),
        DW_AT_priority(0x45),
        DW_AT_segment(0x46),
        DW_AT_specification(0x47),
        DW_AT_static_link(0x48),
        DW_AT_type(0x49),
        DW_AT_use_location(0x4a),
        DW_AT_variable_parameter(0x4b),
        DW_AT_virtuality(0x4c),
        DW_AT_vtable_elem_location(0x4d),
        DW_AT_allocated(0x4e),
        DW_AT_associated(0x4f),
        DW_AT_data_location(0x50),
        DW_AT_byte_stride(0x51),
        DW_AT_entry_pc(0x52),
        DW_AT_use_UTF8(0x53),
        DW_AT_extension(0x54),
        DW_AT_ranges(0x55),
        DW_AT_trampoline(0x56),
        DW_AT_call_column(0x57),
        DW_AT_call_file(0x58),
        DW_AT_call_line(0x59),
        DW_AT_description(0x5a),
        DW_AT_binary_scale(0x5b),
        DW_AT_decimal_scale(0x5c),
        DW_AT_small(0x5d),
        DW_AT_decimal_sign(0x5e),
        DW_AT_digit_count(0x5f),
        DW_AT_picture_string(0x60),
        DW_AT_mutable(0x61),
        DW_AT_threads_scaled(0x62),
        DW_AT_explicit(0x63),
        DW_AT_object_pointer(0x64),
        DW_AT_endianity(0x65),
        DW_AT_elemental(0x66),
        DW_AT_pure(0x67),
        DW_AT_recursive(0x68),
        DW_AT_signature(0x69),
        DW_AT_main_subprogram(0x6a),
        DW_AT_data_bit_offset(0x6b),
        DW_AT_const_expr(0x6c),
        DW_AT_enum_class(0x6d),
        DW_AT_linkage_name(0x6e),
        DW_AT_string_length_bit_size(0x6f),
        DW_AT_string_length_byte_size(0x70),
        DW_AT_rank(0x71),
        DW_AT_str_offsets_base(0x72),
        DW_AT_addr_base(0x73),
        DW_AT_rnglists_base(0x74),
        DW_AT_dwo_id(0x75), ///< Retracted from DWARF v5.
        DW_AT_dwo_name(0x76),
        DW_AT_reference(0x77),
        DW_AT_rvalue_reference(0x78),
        DW_AT_macros(0x79),
        DW_AT_call_all_calls(0x7a),
        DW_AT_call_all_source_calls(0x7b),
        DW_AT_call_all_tail_calls(0x7c),
        DW_AT_call_return_pc(0x7d),
        DW_AT_call_value(0x7e),
        DW_AT_call_origin(0x7f),
        DW_AT_call_parameter(0x80),
        DW_AT_call_pc(0x81),
        DW_AT_call_tail_call(0x82),
        DW_AT_call_target(0x83),
        DW_AT_call_target_clobbered(0x84),
        DW_AT_call_data_location(0x85),
        DW_AT_call_data_value(0x86),
        DW_AT_noreturn(0x87),
        DW_AT_alignment(0x88),
        DW_AT_export_symbols(0x89),
        DW_AT_deleted(0x8a),
        DW_AT_defaulted(0x8b),
        DW_AT_loclists_base(0x8c),
        DW_AT_MIPS_loop_begin(0x2002),
        DW_AT_MIPS_tail_loop_begin(0x2003),
        DW_AT_MIPS_epilog_begin(0x2004),
        DW_AT_MIPS_loop_unroll_factor(0x2005),
        DW_AT_MIPS_software_pipeline_depth(0x2006),
        DW_AT_MIPS_linkage_name(0x2007),
        DW_AT_MIPS_stride(0x2008),
        DW_AT_MIPS_abstract_name(0x2009),
        DW_AT_MIPS_clone_origin(0x200a),
        DW_AT_MIPS_has_inlines(0x200b),
        DW_AT_MIPS_stride_byte(0x200c),
        DW_AT_MIPS_stride_elem(0x200d),
        DW_AT_MIPS_ptr_dopetype(0x200e),
        DW_AT_MIPS_allocatable_dopetype(0x200f),
        DW_AT_MIPS_assumed_shape_dopetype(0x2010),
        DW_AT_MIPS_assumed_size(0x2011),
        DW_AT_sf_names(0x2101),
        DW_AT_src_info(0x2102),
        DW_AT_mac_info(0x2103),
        DW_AT_src_coords(0x2104),
        DW_AT_body_begin(0x2105),
        DW_AT_body_end(0x2106),
        DW_AT_GNU_vector(0x2107),
        DW_AT_GNU_template_name(0x2110),
        DW_AT_GNU_odr_signature(0x210f),
        DW_AT_GNU_call_site_value(0x2111),
        DW_AT_GNU_all_call_sites(0x2117),
        DW_AT_GNU_macros(0x2119),
        DW_AT_GNU_dwo_name(0x2130),
        DW_AT_GNU_dwo_id(0x2131),
        DW_AT_GNU_ranges_base(0x2132),
        DW_AT_GNU_addr_base(0x2133),
        DW_AT_GNU_pubnames(0x2134),
        DW_AT_GNU_pubtypes(0x2135),
        DW_AT_GNU_discriminator(0x2136),
        DW_AT_BORLAND_property_read(0x3b11),
        DW_AT_BORLAND_property_write(0x3b12),
        DW_AT_BORLAND_property_implements(0x3b13),
        DW_AT_BORLAND_property_index(0x3b14),
        DW_AT_BORLAND_property_default(0x3b15),
        DW_AT_BORLAND_Delphi_unit(0x3b20),
        DW_AT_BORLAND_Delphi_class(0x3b21),
        DW_AT_BORLAND_Delphi_record(0x3b22),
        DW_AT_BORLAND_Delphi_metaclass(0x3b23),
        DW_AT_BORLAND_Delphi_constructor(0x3b24),
        DW_AT_BORLAND_Delphi_destructor(0x3b25),
        DW_AT_BORLAND_Delphi_anonymous_method(0x3b26),
        DW_AT_BORLAND_Delphi_interface(0x3b27),
        DW_AT_BORLAND_Delphi_ABI(0x3b28),
        DW_AT_BORLAND_Delphi_return(0x3b29),
        DW_AT_BORLAND_Delphi_frameptr(0x3b30),
        DW_AT_BORLAND_closure(0x3b31),
        DW_AT_LLVM_include_path(0x3e00),
        DW_AT_LLVM_config_macros(0x3e01),
        DW_AT_LLVM_isysroot(0x3e02),
        DW_AT_APPLE_optimized(0x3fe1),
        DW_AT_APPLE_flags(0x3fe2),
        DW_AT_APPLE_isa(0x3fe3),
        DW_AT_APPLE_block(0x3fe4),
        DW_AT_APPLE_major_runtime_vers(0x3fe5),
        DW_AT_APPLE_runtime_class(0x3fe6),
        DW_AT_APPLE_omit_frame_ptr(0x3fe7),
        DW_AT_APPLE_property_name(0x3fe8),
        DW_AT_APPLE_property_getter(0x3fe9),
        DW_AT_APPLE_property_setter(0x3fea),
        DW_AT_APPLE_property_attribute(0x3feb),
        DW_AT_APPLE_objc_complete_type(0x3fec),
        DW_AT_APPLE_property(0x3fed),
    }

    companion object {
        fun by(name: String, rawValue: String): DwarfAttribute = by(Attribute.valueOf(name), rawValue)
        fun by(attribute: Attribute, rawValue: String) = DwarfAttribute(attribute, rawValue)
    }
}

val DwarfAttribute.rvString: String
    get() = rawValue.trim().substring(1, rawValue.length - 2)


class DwarfUtilParser() {
    val tags = mutableListOf<DwarfTag>()
    var currentTag: DwarfTag? = null
    var currentAttribute: DwarfAttribute.Attribute? = null
    val currentAttributePayload = StringBuilder()
    companion object {
        val tagRegexp = Regex("^(0x[0-9a-f]{8}):\\ {3}(.*)$")
        val attributeRegexp = Regex("^\\s+(DW_AT_[a-zA-Z0-9_]*)\\s+\\((.*)\\)$")
    }

    fun tag(tag: DwarfTag) {
        appendCurrentAttribute()
        tags.add(tag)
        currentTag = tag
        currentAttribute = null
    }

    private fun appendCurrentAttribute() {
        currentTag ?: return
        with(currentTag!!) {
            currentAttribute ?: return
            +DwarfAttribute.by(currentAttribute!!, currentAttributePayload.toString())
            currentAttributePayload.clear()
        }
    }

    fun attribute(attribute: DwarfAttribute.Attribute, payload: String) {
        appendCurrentAttribute()
        currentAttribute = attribute
        currentAttributePayload.appendLine(payload)
    }

    fun parse(reader: Reader): DwarfUtilParser {
        reader.forEachLine { line ->
            tagRegexp.find(line)?.apply {
                val str = this.destructured.component2().trim()
                if (str == "NULL")
                    return@forEachLine
                tag(DwarfTag.by(str))
                return@forEachLine
            }

            attributeRegexp.find(line)?.apply {
                attribute(DwarfAttribute.Attribute.valueOf(destructured.component1()), destructured.component2())
                return@forEachLine
            }

            currentTag ?: return@forEachLine
            currentAttributePayload.appendLine(line)
        }
        return this
    }
}
