/**
  ******************************************************************************
  * @file    stm32f4xx_fsmc.h
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file contains all the functions prototypes for the FSMC firmware 
  *          library.
  ******************************************************************************
  * @attention
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ******************************************************************************
  */

/* Define to prevent recursive inclusion -------------------------------------*/
#ifndef __STM32F4xx_FSMC_H
#define __STM32F4xx_FSMC_H

#ifdef __cplusplus
 extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @addtogroup FSMC
  * @{
  */

/* Exported types ------------------------------------------------------------*/

/** 
  * @brief  Timing parameters For NOR/SRAM Banks  
  */
typedef struct
{
  uint32_t FSMC_AddressSetupTime;       /*!< Defines the number of HCLK cycles to configure
                                             the duration of the address setup time. 
                                             This parameter can be a value between 0 and 0xF.
                                             @note This parameter is not used with synchronous NOR Flash memories. */

  uint32_t FSMC_AddressHoldTime;        /*!< Defines the number of HCLK cycles to configure
                                             the duration of the address hold time.
                                             This parameter can be a value between 0 and 0xF. 
                                             @note This parameter is not used with synchronous NOR Flash memories.*/

  uint32_t FSMC_DataSetupTime;          /*!< Defines the number of HCLK cycles to configure
                                             the duration of the data setup time.
                                             This parameter can be a value between 0 and 0xFF.
                                             @note This parameter is used for SRAMs, ROMs and asynchronous multiplexed NOR Flash memories. */

  uint32_t FSMC_BusTurnAroundDuration;  /*!< Defines the number of HCLK cycles to configure
                                             the duration of the bus turnaround.
                                             This parameter can be a value between 0 and 0xF.
                                             @note This parameter is only used for multiplexed NOR Flash memories. */

  uint32_t FSMC_CLKDivision;            /*!< Defines the period of CLK clock output signal, expressed in number of HCLK cycles.
                                             This parameter can be a value between 1 and 0xF.
                                             @note This parameter is not used for asynchronous NOR Flash, SRAM or ROM accesses. */

  uint32_t FSMC_DataLatency;            /*!< Defines the number of memory clock cycles to issue
                                             to the memory before getting the first data.
                                             The parameter value depends on the memory type as shown below:
                                              - It must be set to 0 in case of a CRAM
                                              - It is don't care in asynchronous NOR, SRAM or ROM accesses
                                              - It may assume a value between 0 and 0xF in NOR Flash memories
                                                with synchronous burst mode enable */

  uint32_t FSMC_AccessMode;             /*!< Specifies the asynchronous access mode. 
                                             This parameter can be a value of @ref FSMC_Access_Mode */
}FSMC_NORSRAMTimingInitTypeDef;

/** 
  * @brief  FSMC NOR/SRAM Init structure definition
  */
typedef struct
{
  uint32_t FSMC_Bank;                /*!< Specifies the NOR/SRAM memory bank that will be used.
                                          This parameter can be a value of @ref FSMC_NORSRAM_Bank */

  uint32_t FSMC_DataAddressMux;      /*!< Specifies whether the address and data values are
                                          multiplexed on the databus or not. 
                                          This parameter can be a value of @ref FSMC_Data_Address_Bus_Multiplexing */

  uint32_t FSMC_MemoryType;          /*!< Specifies the type of external memory attached to
                                          the corresponding memory bank.
                                          This parameter can be a value of @ref FSMC_Memory_Type */

  uint32_t FSMC_MemoryDataWidth;     /*!< Specifies the external memory device width.
                                          This parameter can be a value of @ref FSMC_Data_Width */

  uint32_t FSMC_BurstAccessMode;     /*!< Enables or disables the burst access mode for Flash memory,
                                          valid only with synchronous burst Flash memories.
                                          This parameter can be a value of @ref FSMC_Burst_Access_Mode */

  uint32_t FSMC_AsynchronousWait;     /*!< Enables or disables wait signal during asynchronous transfers,
                                          valid only with asynchronous Flash memories.
                                          This parameter can be a value of @ref FSMC_AsynchronousWait */                                          

  uint32_t FSMC_WaitSignalPolarity;  /*!< Specifies the wait signal polarity, valid only when accessing
                                          the Flash memory in burst mode.
                                          This parameter can be a value of @ref FSMC_Wait_Signal_Polarity */

  uint32_t FSMC_WrapMode;            /*!< Enables or disables the Wrapped burst access mode for Flash
                                          memory, valid only when accessing Flash memories in burst mode.
                                          This parameter can be a value of @ref FSMC_Wrap_Mode */

  uint32_t FSMC_WaitSignalActive;    /*!< Specifies if the wait signal is asserted by the memory one
                                          clock cycle before the wait state or during the wait state,
                                          valid only when accessing memories in burst mode. 
                                          This parameter can be a value of @ref FSMC_Wait_Timing */

  uint32_t FSMC_WriteOperation;      /*!< Enables or disables the write operation in the selected bank by the FSMC. 
                                          This parameter can be a value of @ref FSMC_Write_Operation */

  uint32_t FSMC_WaitSignal;          /*!< Enables or disables the wait-state insertion via wait
                                          signal, valid for Flash memory access in burst mode. 
                                          This parameter can be a value of @ref FSMC_Wait_Signal */

  uint32_t FSMC_ExtendedMode;        /*!< Enables or disables the extended mode.
                                          This parameter can be a value of @ref FSMC_Extended_Mode */

  uint32_t FSMC_WriteBurst;          /*!< Enables or disables the write burst operation.
                                          This parameter can be a value of @ref FSMC_Write_Burst */ 

  FSMC_NORSRAMTimingInitTypeDef* FSMC_ReadWriteTimingStruct; /*!< Timing Parameters for write and read access if the  ExtendedMode is not used*/  

  FSMC_NORSRAMTimingInitTypeDef* FSMC_WriteTimingStruct;     /*!< Timing Parameters for write access if the  ExtendedMode is used*/      
}FSMC_NORSRAMInitTypeDef;

/** 
  * @brief  Timing parameters For FSMC NAND and PCCARD Banks
  */
typedef struct
{
  uint32_t FSMC_SetupTime;      /*!< Defines the number of HCLK cycles to setup address before
                                     the command assertion for NAND-Flash read or write access
                                     to common/Attribute or I/O memory space (depending on
                                     the memory space timing to be configured).
                                     This parameter can be a value between 0 and 0xFF.*/

  uint32_t FSMC_WaitSetupTime;  /*!< Defines the minimum number of HCLK cycles to assert the
                                     command for NAND-Flash read or write access to
                                     common/Attribute or I/O memory space (depending on the
                                     memory space timing to be configured). 
                                     This parameter can be a number between 0x00 and 0xFF */

  uint32_t FSMC_HoldSetupTime;  /*!< Defines the number of HCLK clock cycles to hold address
                                     (and data for write access) after the command deassertion
                                     for NAND-Flash read or write access to common/Attribute
                                     or I/O memory space (depending on the memory space timing
                                     to be configured).
                                     This parameter can be a number between 0x00 and 0xFF */

  uint32_t FSMC_HiZSetupTime;   /*!< Defines the number of HCLK clock cycles during which the
                                     databus is kept in HiZ after the start of a NAND-Flash
                                     write access to common/Attribute or I/O memory space (depending
                                     on the memory space timing to be configured).
                                     This parameter can be a number between 0x00 and 0xFF */
}FSMC_NAND_PCCARDTimingInitTypeDef;

/** 
  * @brief  FSMC NAND Init structure definition
  */
typedef struct
{
  uint32_t FSMC_Bank;              /*!< Specifies the NAND memory bank that will be used.
                                      This parameter can be a value of @ref FSMC_NAND_Bank */

  uint32_t FSMC_Waitfeature;      /*!< Enables or disables the Wait feature for the NAND Memory Bank.
                                       This parameter can be any value of @ref FSMC_Wait_feature */

  uint32_t FSMC_MemoryDataWidth;  /*!< Specifies the external memory device width.
                                       This parameter can be any value of @ref FSMC_Data_Width */

  uint32_t FSMC_ECC;              /*!< Enables or disables the ECC computation.
                                       This parameter can be any value of @ref FSMC_ECC */

  uint32_t FSMC_ECCPageSize;      /*!< Defines the page size for the extended ECC.
                                       This parameter can be any value of @ref FSMC_ECC_Page_Size */

  uint32_t FSMC_TCLRSetupTime;    /*!< Defines the number of HCLK cycles to configure the
                                       delay between CLE low and RE low.
                                       This parameter can be a value between 0 and 0xFF. */

  uint32_t FSMC_TARSetupTime;     /*!< Defines the number of HCLK cycles to configure the
                                       delay between ALE low and RE low.
                                       This parameter can be a number between 0x0 and 0xFF */ 

  FSMC_NAND_PCCARDTimingInitTypeDef*  FSMC_CommonSpaceTimingStruct;   /*!< FSMC Common Space Timing */ 

  FSMC_NAND_PCCARDTimingInitTypeDef*  FSMC_AttributeSpaceTimingStruct; /*!< FSMC Attribute Space Timing */
}FSMC_NANDInitTypeDef;

/** 
  * @brief  FSMC PCCARD Init structure definition
  */

typedef struct
{
  uint32_t FSMC_Waitfeature;    /*!< Enables or disables the Wait feature for the Memory Bank.
                                    This parameter can be any value of @ref FSMC_Wait_feature */

  uint32_t FSMC_TCLRSetupTime;  /*!< Defines the number of HCLK cycles to configure the
                                     delay between CLE low and RE low.
                                     This parameter can be a value between 0 and 0xFF. */

  uint32_t FSMC_TARSetupTime;   /*!< Defines the number of HCLK cycles to configure the
                                     delay between ALE low and RE low.
                                     This parameter can be a number between 0x0 and 0xFF */ 

  
  FSMC_NAND_PCCARDTimingInitTypeDef*  FSMC_CommonSpaceTimingStruct; /*!< FSMC Common Space Timing */

  FSMC_NAND_PCCARDTimingInitTypeDef*  FSMC_AttributeSpaceTimingStruct;  /*!< FSMC Attribute Space Timing */ 
  
  FSMC_NAND_PCCARDTimingInitTypeDef*  FSMC_IOSpaceTimingStruct; /*!< FSMC IO Space Timing */  
}FSMC_PCCARDInitTypeDef;

/* Exported constants --------------------------------------------------------*/

/** @defgroup FSMC_Exported_Constants
  * @{
  */

/** @defgroup FSMC_NORSRAM_Bank 
  * @{
  */
#define FSMC_Bank1_NORSRAM1                      ((uint32_t)0x00000000)
#define FSMC_Bank1_NORSRAM2                      ((uint32_t)0x00000002)
#define FSMC_Bank1_NORSRAM3                      ((uint32_t)0x00000004)
#define FSMC_Bank1_NORSRAM4                      ((uint32_t)0x00000006)
/**
  * @}
  */

/** @defgroup FSMC_NAND_Bank 
  * @{
  */  
#define FSMC_Bank2_NAND                          ((uint32_t)0x00000010)
#define FSMC_Bank3_NAND                          ((uint32_t)0x00000100)
/**
  * @}
  */

/** @defgroup FSMC_PCCARD_Bank 
  * @{
  */    
#define FSMC_Bank4_PCCARD                        ((uint32_t)0x00001000)
/**
  * @}
  */

#define IS_FSMC_NORSRAM_BANK(BANK) (((BANK) == FSMC_Bank1_NORSRAM1) || \
                                    ((BANK) == FSMC_Bank1_NORSRAM2) || \
                                    ((BANK) == FSMC_Bank1_NORSRAM3) || \
                                    ((BANK) == FSMC_Bank1_NORSRAM4))

#define IS_FSMC_NAND_BANK(BANK) (((BANK) == FSMC_Bank2_NAND) || \
                                 ((BANK) == FSMC_Bank3_NAND))

#define IS_FSMC_GETFLAG_BANK(BANK) (((BANK) == FSMC_Bank2_NAND) || \
                                    ((BANK) == FSMC_Bank3_NAND) || \
                                    ((BANK) == FSMC_Bank4_PCCARD))

#define IS_FSMC_IT_BANK(BANK) (((BANK) == FSMC_Bank2_NAND) || \
                               ((BANK) == FSMC_Bank3_NAND) || \
                               ((BANK) == FSMC_Bank4_PCCARD))

/** @defgroup FSMC_NOR_SRAM_Controller 
  * @{
  */

/** @defgroup FSMC_Data_Address_Bus_Multiplexing 
  * @{
  */

#define FSMC_DataAddressMux_Disable                ((uint32_t)0x00000000)
#define FSMC_DataAddressMux_Enable                 ((uint32_t)0x00000002)
#define IS_FSMC_MUX(MUX) (((MUX) == FSMC_DataAddressMux_Disable) || \
                          ((MUX) == FSMC_DataAddressMux_Enable))
/**
  * @}
  */

/** @defgroup FSMC_Memory_Type 
  * @{
  */

#define FSMC_MemoryType_SRAM                     ((uint32_t)0x00000000)
#define FSMC_MemoryType_PSRAM                    ((uint32_t)0x00000004)
#define FSMC_MemoryType_NOR                      ((uint32_t)0x00000008)
#define IS_FSMC_MEMORY(MEMORY) (((MEMORY) == FSMC_MemoryType_SRAM) || \
                                ((MEMORY) == FSMC_MemoryType_PSRAM)|| \
                                ((MEMORY) == FSMC_MemoryType_NOR))
/**
  * @}
  */

/** @defgroup FSMC_Data_Width 
  * @{
  */

#define FSMC_MemoryDataWidth_8b                  ((uint32_t)0x00000000)
#define FSMC_MemoryDataWidth_16b                 ((uint32_t)0x00000010)
#define IS_FSMC_MEMORY_WIDTH(WIDTH) (((WIDTH) == FSMC_MemoryDataWidth_8b) || \
                                     ((WIDTH) == FSMC_MemoryDataWidth_16b))
/**
  * @}
  */

/** @defgroup FSMC_Burst_Access_Mode 
  * @{
  */

#define FSMC_BurstAccessMode_Disable             ((uint32_t)0x00000000) 
#define FSMC_BurstAccessMode_Enable              ((uint32_t)0x00000100)
#define IS_FSMC_BURSTMODE(STATE) (((STATE) == FSMC_BurstAccessMode_Disable) || \
                                  ((STATE) == FSMC_BurstAccessMode_Enable))
/**
  * @}
  */
    
/** @defgroup FSMC_AsynchronousWait 
  * @{
  */
#define FSMC_AsynchronousWait_Disable            ((uint32_t)0x00000000)
#define FSMC_AsynchronousWait_Enable             ((uint32_t)0x00008000)
#define IS_FSMC_ASYNWAIT(STATE) (((STATE) == FSMC_AsynchronousWait_Disable) || \
                                 ((STATE) == FSMC_AsynchronousWait_Enable))
/**
  * @}
  */

/** @defgroup FSMC_Wait_Signal_Polarity 
  * @{
  */
#define FSMC_WaitSignalPolarity_Low              ((uint32_t)0x00000000)
#define FSMC_WaitSignalPolarity_High             ((uint32_t)0x00000200)
#define IS_FSMC_WAIT_POLARITY(POLARITY) (((POLARITY) == FSMC_WaitSignalPolarity_Low) || \
                                         ((POLARITY) == FSMC_WaitSignalPolarity_High))
/**
  * @}
  */

/** @defgroup FSMC_Wrap_Mode 
  * @{
  */
#define FSMC_WrapMode_Disable                    ((uint32_t)0x00000000)
#define FSMC_WrapMode_Enable                     ((uint32_t)0x00000400) 
#define IS_FSMC_WRAP_MODE(MODE) (((MODE) == FSMC_WrapMode_Disable) || \
                                 ((MODE) == FSMC_WrapMode_Enable))
/**
  * @}
  */

/** @defgroup FSMC_Wait_Timing 
  * @{
  */
#define FSMC_WaitSignalActive_BeforeWaitState    ((uint32_t)0x00000000)
#define FSMC_WaitSignalActive_DuringWaitState    ((uint32_t)0x00000800) 
#define IS_FSMC_WAIT_SIGNAL_ACTIVE(ACTIVE) (((ACTIVE) == FSMC_WaitSignalActive_BeforeWaitState) || \
                                            ((ACTIVE) == FSMC_WaitSignalActive_DuringWaitState))
/**
  * @}
  */

/** @defgroup FSMC_Write_Operation 
  * @{
  */
#define FSMC_WriteOperation_Disable                     ((uint32_t)0x00000000)
#define FSMC_WriteOperation_Enable                      ((uint32_t)0x00001000)
#define IS_FSMC_WRITE_OPERATION(OPERATION) (((OPERATION) == FSMC_WriteOperation_Disable) || \
                                            ((OPERATION) == FSMC_WriteOperation_Enable))                         
/**
  * @}
  */

/** @defgroup FSMC_Wait_Signal 
  * @{
  */
#define FSMC_WaitSignal_Disable                  ((uint32_t)0x00000000)
#define FSMC_WaitSignal_Enable                   ((uint32_t)0x00002000) 
#define IS_FSMC_WAITE_SIGNAL(SIGNAL) (((SIGNAL) == FSMC_WaitSignal_Disable) || \
                                      ((SIGNAL) == FSMC_WaitSignal_Enable))
/**
  * @}
  */

/** @defgroup FSMC_Extended_Mode 
  * @{
  */
#define FSMC_ExtendedMode_Disable                ((uint32_t)0x00000000)
#define FSMC_ExtendedMode_Enable                 ((uint32_t)0x00004000)

#define IS_FSMC_EXTENDED_MODE(MODE) (((MODE) == FSMC_ExtendedMode_Disable) || \
                                     ((MODE) == FSMC_ExtendedMode_Enable)) 
/**
  * @}
  */

/** @defgroup FSMC_Write_Burst 
  * @{
  */

#define FSMC_WriteBurst_Disable                  ((uint32_t)0x00000000)
#define FSMC_WriteBurst_Enable                   ((uint32_t)0x00080000) 
#define IS_FSMC_WRITE_BURST(BURST) (((BURST) == FSMC_WriteBurst_Disable) || \
                                    ((BURST) == FSMC_WriteBurst_Enable))
/**
  * @}
  */

/** @defgroup FSMC_Address_Setup_Time 
  * @{
  */
#define IS_FSMC_ADDRESS_SETUP_TIME(TIME) ((TIME) <= 0xF)
/**
  * @}
  */

/** @defgroup FSMC_Address_Hold_Time 
  * @{
  */
#define IS_FSMC_ADDRESS_HOLD_TIME(TIME) ((TIME) <= 0xF)
/**
  * @}
  */

/** @defgroup FSMC_Data_Setup_Time 
  * @{
  */
#define IS_FSMC_DATASETUP_TIME(TIME) (((TIME) > 0) && ((TIME) <= 0xFF))
/**
  * @}
  */

/** @defgroup FSMC_Bus_Turn_around_Duration 
  * @{
  */
#define IS_FSMC_TURNAROUND_TIME(TIME) ((TIME) <= 0xF)
/**
  * @}
  */

/** @defgroup FSMC_CLK_Division 
  * @{
  */
#define IS_FSMC_CLK_DIV(DIV) ((DIV) <= 0xF)
/**
  * @}
  */

/** @defgroup FSMC_Data_Latency 
  * @{
  */
#define IS_FSMC_DATA_LATENCY(LATENCY) ((LATENCY) <= 0xF)
/**
  * @}
  */

/** @defgroup FSMC_Access_Mode 
  * @{
  */
#define FSMC_AccessMode_A                        ((uint32_t)0x00000000)
#define FSMC_AccessMode_B                        ((uint32_t)0x10000000) 
#define FSMC_AccessMode_C                        ((uint32_t)0x20000000)
#define FSMC_AccessMode_D                        ((uint32_t)0x30000000)
#define IS_FSMC_ACCESS_MODE(MODE) (((MODE) == FSMC_AccessMode_A) || \
                                   ((MODE) == FSMC_AccessMode_B) || \
                                   ((MODE) == FSMC_AccessMode_C) || \
                                   ((MODE) == FSMC_AccessMode_D))
/**
  * @}
  */

/**
  * @}
  */
  
/** @defgroup FSMC_NAND_PCCARD_Controller 
  * @{
  */

/** @defgroup FSMC_Wait_feature 
  * @{
  */
#define FSMC_Waitfeature_Disable                 ((uint32_t)0x00000000)
#define FSMC_Waitfeature_Enable                  ((uint32_t)0x00000002)
#define IS_FSMC_WAIT_FEATURE(FEATURE) (((FEATURE) == FSMC_Waitfeature_Disable) || \
                                       ((FEATURE) == FSMC_Waitfeature_Enable))
/**
  * @}
  */


/** @defgroup FSMC_ECC 
  * @{
  */
#define FSMC_ECC_Disable                         ((uint32_t)0x00000000)
#define FSMC_ECC_Enable                          ((uint32_t)0x00000040)
#define IS_FSMC_ECC_STATE(STATE) (((STATE) == FSMC_ECC_Disable) || \
                                  ((STATE) == FSMC_ECC_Enable))
/**
  * @}
  */

/** @defgroup FSMC_ECC_Page_Size 
  * @{
  */
#define FSMC_ECCPageSize_256Bytes                ((uint32_t)0x00000000)
#define FSMC_ECCPageSize_512Bytes                ((uint32_t)0x00020000)
#define FSMC_ECCPageSize_1024Bytes               ((uint32_t)0x00040000)
#define FSMC_ECCPageSize_2048Bytes               ((uint32_t)0x00060000)
#define FSMC_ECCPageSize_4096Bytes               ((uint32_t)0x00080000)
#define FSMC_ECCPageSize_8192Bytes               ((uint32_t)0x000A0000)
#define IS_FSMC_ECCPAGE_SIZE(SIZE) (((SIZE) == FSMC_ECCPageSize_256Bytes) || \
                                    ((SIZE) == FSMC_ECCPageSize_512Bytes) || \
                                    ((SIZE) == FSMC_ECCPageSize_1024Bytes) || \
                                    ((SIZE) == FSMC_ECCPageSize_2048Bytes) || \
                                    ((SIZE) == FSMC_ECCPageSize_4096Bytes) || \
                                    ((SIZE) == FSMC_ECCPageSize_8192Bytes))
/**
  * @}
  */

/** @defgroup FSMC_TCLR_Setup_Time 
  * @{
  */
#define IS_FSMC_TCLR_TIME(TIME) ((TIME) <= 0xFF)
/**
  * @}
  */

/** @defgroup FSMC_TAR_Setup_Time 
  * @{
  */
#define IS_FSMC_TAR_TIME(TIME) ((TIME) <= 0xFF)
/**
  * @}
  */

/** @defgroup FSMC_Setup_Time 
  * @{
  */
#define IS_FSMC_SETUP_TIME(TIME) ((TIME) <= 0xFF)
/**
  * @}
  */

/** @defgroup FSMC_Wait_Setup_Time 
  * @{
  */
#define IS_FSMC_WAIT_TIME(TIME) ((TIME) <= 0xFF)
/**
  * @}
  */

/** @defgroup FSMC_Hold_Setup_Time 
  * @{
  */
#define IS_FSMC_HOLD_TIME(TIME) ((TIME) <= 0xFF)
/**
  * @}
  */

/** @defgroup FSMC_HiZ_Setup_Time 
  * @{
  */
#define IS_FSMC_HIZ_TIME(TIME) ((TIME) <= 0xFF)
/**
  * @}
  */

/** @defgroup FSMC_Interrupt_sources 
  * @{
  */
#define FSMC_IT_RisingEdge                       ((uint32_t)0x00000008)
#define FSMC_IT_Level                            ((uint32_t)0x00000010)
#define FSMC_IT_FallingEdge                      ((uint32_t)0x00000020)
#define IS_FSMC_IT(IT) ((((IT) & (uint32_t)0xFFFFFFC7) == 0x00000000) && ((IT) != 0x00000000))
#define IS_FSMC_GET_IT(IT) (((IT) == FSMC_IT_RisingEdge) || \
                            ((IT) == FSMC_IT_Level) || \
                            ((IT) == FSMC_IT_FallingEdge)) 
/**
  * @}
  */

/** @defgroup FSMC_Flags 
  * @{
  */
#define FSMC_FLAG_RisingEdge                     ((uint32_t)0x00000001)
#define FSMC_FLAG_Level                          ((uint32_t)0x00000002)
#define FSMC_FLAG_FallingEdge                    ((uint32_t)0x00000004)
#define FSMC_FLAG_FEMPT                          ((uint32_t)0x00000040)
#define IS_FSMC_GET_FLAG(FLAG) (((FLAG) == FSMC_FLAG_RisingEdge) || \
                                ((FLAG) == FSMC_FLAG_Level) || \
                                ((FLAG) == FSMC_FLAG_FallingEdge) || \
                                ((FLAG) == FSMC_FLAG_FEMPT))

#define IS_FSMC_CLEAR_FLAG(FLAG) ((((FLAG) & (uint32_t)0xFFFFFFF8) == 0x00000000) && ((FLAG) != 0x00000000))
/**
  * @}
  */

/**
  * @}
  */

/**
  * @}
  */

/* Exported macro ------------------------------------------------------------*/
/* Exported functions --------------------------------------------------------*/ 

/* NOR/SRAM Controller functions **********************************************/
void FSMC_NORSRAMDeInit(uint32_t FSMC_Bank);
void FSMC_NORSRAMInit(FSMC_NORSRAMInitTypeDef* FSMC_NORSRAMInitStruct);
void FSMC_NORSRAMStructInit(FSMC_NORSRAMInitTypeDef* FSMC_NORSRAMInitStruct);
void FSMC_NORSRAMCmd(uint32_t FSMC_Bank, FunctionalState NewState);

/* NAND Controller functions **************************************************/
void FSMC_NANDDeInit(uint32_t FSMC_Bank);
void FSMC_NANDInit(FSMC_NANDInitTypeDef* FSMC_NANDInitStruct);
void FSMC_NANDStructInit(FSMC_NANDInitTypeDef* FSMC_NANDInitStruct);
void FSMC_NANDCmd(uint32_t FSMC_Bank, FunctionalState NewState);
void FSMC_NANDECCCmd(uint32_t FSMC_Bank, FunctionalState NewState);
uint32_t FSMC_GetECC(uint32_t FSMC_Bank);

/* PCCARD Controller functions ************************************************/
void FSMC_PCCARDDeInit(void);
void FSMC_PCCARDInit(FSMC_PCCARDInitTypeDef* FSMC_PCCARDInitStruct);
void FSMC_PCCARDStructInit(FSMC_PCCARDInitTypeDef* FSMC_PCCARDInitStruct);
void FSMC_PCCARDCmd(FunctionalState NewState);

/* Interrupts and flags management functions **********************************/
void FSMC_ITConfig(uint32_t FSMC_Bank, uint32_t FSMC_IT, FunctionalState NewState);
FlagStatus FSMC_GetFlagStatus(uint32_t FSMC_Bank, uint32_t FSMC_FLAG);
void FSMC_ClearFlag(uint32_t FSMC_Bank, uint32_t FSMC_FLAG);
ITStatus FSMC_GetITStatus(uint32_t FSMC_Bank, uint32_t FSMC_IT);
void FSMC_ClearITPendingBit(uint32_t FSMC_Bank, uint32_t FSMC_IT);

#ifdef __cplusplus
}
#endif

#endif /*__STM32F4xx_FSMC_H */
/**
  * @}
  */

/**
  * @}
  */ 

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
